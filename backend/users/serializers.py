from rest_framework import serializers
from django.contrib.auth import get_user_model
import re

User = get_user_model()


class RegisterSerializer(serializers.ModelSerializer):
    """Serializer para criacao de conta."""
    password = serializers.Charfield(write_only=True, min_length=6)
    
    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'password', 'bio', 'avatar']
        
    def validate_username(self, value):
        if not re.match(r'^\w+$', value):
            raise serializers.ValidationError(
                'Username so pode conter letras, numeros e _'
            )
        return value
    
    def validate_email(self, value):
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError('Este email ja esta cadastrado.')
        return value
    
    def create(self, validated_data):
        #created_user ja faz o hash da senha insta
        user = User.objects.create_user(
            username=validated_data['username'],
            email=validated_data['email'],
            password=validated_data['password'],
            bio=validated_data.get('bio', ''),
            avatar=validated_data.get('avatar', '')
        )
        return user
    
    
class UserSerializer(serializers.ModelSerializer):
    """Serializer para leituras de dados do usuario."""
    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'bio', 'avatar', 'created_at']
        read_only_fields = ['id', 'created_at']