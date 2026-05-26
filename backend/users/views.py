from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth import get_user_model
from django.db.models import Q

from .serializers import RegisterSerializer, UserSerializer

User = get_user_model()


def get_token_for_user(user):
    """Gera access token e refresh token para um usuario."""
    refresh = RefreshToken.for_user(user)
    return {
        'refresh': str(refresh),
        'access': str(refresh.access_token),
    }
    

@api_view(['POST'])
@permission_classes([AllowAny])
def register(request):
    """
    POST /auth/register
    Cria uma nova conta de usuario.
    """
    serializer = RegisterSerializer(data=request.data)
    
    if serializer.is_valid():
        user = serializer.save()
        tokens = get_token_for_user(user)
        return Response({
            'user': UserSerializer(user).data,
            'tokens': tokens,
        }, status=status.HTTP_201_CREATED)
        
    return Response(serializer.erros, status=status.HTTP_400_BAD_REQUEST)


@api_view(['POST'])
@permission_classes([AllowAny])
def login(request):
    """
    POST /auth/login
    Autentica com email ou username + senha.
    """
    identifier = request.data.get('identifier', '').strip()
    password = request.data.get('password', '')
    
    if not identifier or not password:
        return Response(
            {'error': 'Informe o email ou username e a senha'},
            status = status.HTTP_400_BAD_REQUEST
        )
        
    #busca por email ou username
    user = User.objects.filter(
        Q(email=identifier) | Q(username=identifier)
    ).first()
    
    if user is None or not user.check_password(password):
        return Response(
            {'error': 'Usuario ou senha invalidado'},
            status=status.HTTP_401_UNAUTHORIZED
        )
        
    tokens = get_token_for_user(user)
    return Response({
        'user': UserSerializer(user).data,
        'tokens': tokens,
    }, status=status.HTTP_200_OK)
    
    
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def me(request):
    """
    GET /auth/me
    Retorna os dados do usuario autenticado
    Requer token JWT no header
    """
    serializer = UserSerializer(request.user)
    return Response(serializer.data, status=status.HTTP_200_OK)