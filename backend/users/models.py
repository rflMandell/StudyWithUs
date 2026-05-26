from django.contrib.auth.models import AbstractUser
from django.db import models
import re


class User(AbstractUser):
    """
    Modelo de usuario customizado do StudyWithUs.
    Estende o User padrao do Django adicionando avatar e bio.
    """
    email = models.EmailField(unique=True)
    bio = models.CharField(max_length=200, blank=True, default='')
    avatar = models.URLField(blank=True, default='')
    create_at = models.DateTimeField(auto_now_add=True)
    
    # Usamos email ou username para login
    USERNAME_FIELD = 'email'
    #campos obirgatorios alem do email e senha
    REQUIRED_FIELDS = ['username']
    
    def save(self, *args, **kwargs):
        # garante que o username so tenha letras, numeros e _
        if not re.match(r'^\w+$', self.username):
            raise ValueError('Username so pode conter letras, numeros e _')
        super().save(*args, **kwargs)
        
    def __str__(self):
        return self.username