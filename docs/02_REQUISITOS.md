# StudyWithUs --- Requisitos Funcionais e Não Funcionais

## 1. Requisitos funcionais

### RF-01 --- Autenticação

O sistema deve permitir:

-   cadastro;
-   login;
-   logout;
-   refresh token;
-   autenticação via Google;
-   recuperação/evolução futura de credenciais.

### RF-02 --- Perfil

O usuário deve poder:

-   visualizar perfil;
-   editar informações;
-   adicionar avatar;
-   visualizar pontos;
-   visualizar streak;
-   visualizar badges;
-   visualizar comunidades.

### RF-03 --- Social

O sistema deve permitir:

-   seguir usuário;
-   deixar de seguir;
-   listar seguidores;
-   listar usuários seguidos.

### RF-04 --- Comunidades

Usuários devem poder:

-   criar comunidade;
-   visualizar comunidades;
-   explorar comunidades públicas;
-   entrar em comunidades públicas;
-   solicitar entrada em privadas;
-   sair;
-   visualizar membros.

### RF-05 --- Administração de comunidade

Papéis:

``` text
OWNER
ADMIN
MODERATOR
MEMBER
```

Deve ser possível:

-   alterar papéis;
-   remover membros;
-   aprovar solicitações;
-   rejeitar solicitações;
-   moderar conteúdo conforme permissão.

### RF-06 --- Check-in

Um usuário autenticado e membro da comunidade deve poder criar
check-ins.

Um check-in pertence a:

``` text
1 usuário
1 comunidade
```

Pode possuir mídia conforme o plano/regra vigente.

### RF-07 --- Pontos

A criação de atividades válidas deve gerar transações de pontos.

Os pontos devem possuir histórico.

Tipos:

``` text
CHECKIN
CHALLENGE
BONUS
REVERSAL
ADMIN_ADJUSTMENT
```

### RF-08 --- Streak

O sistema deve calcular:

-   streak atual;
-   maior streak;
-   última atividade.

### RF-09 --- Ranking

A plataforma deve permitir rankings:

-   semanais;
-   semestrais;
-   anuais;
-   permanentes.

A configuração deve ser associada à comunidade.

### RF-10 --- Feed

O usuário deve visualizar atividades relevantes de:

-   comunidades;
-   usuários seguidos;
-   demais fontes que venham a ser adicionadas.

O feed inicialmente não exige tabela própria.

### RF-11 --- Comentários

Usuários podem:

-   comentar check-ins;
-   visualizar comentários;
-   remover seus comentários.

Moderadores possuem permissões adicionais.

### RF-12 --- Desafios

O sistema deve permitir:

-   criar desafios;
-   participar;
-   acompanhar progresso;
-   completar;
-   receber recompensas.

### RF-13 --- Badges

O sistema deve permitir conceder badges e listar badges de usuários.

### RF-14 --- Notificações

O sistema deve permitir:

-   listar notificações;
-   marcar como lida;
-   marcar todas como lidas;
-   configurar preferências;
-   registrar dispositivos.

### RF-15 --- Moderação

Usuários podem denunciar conteúdo.

Administradores podem:

-   listar denúncias;
-   analisar;
-   resolver;
-   registrar auditoria.

### RF-16 --- Premium

O sistema deve suportar:

-   assinatura;
-   consulta de plano;
-   cancelamento;
-   webhook de pagamento;
-   eventos de assinatura.

## 2. Regras de negócio importantes

### RB-01

Usuário não pode seguir a si mesmo.

### RB-02

Check-in exige autenticação.

### RB-03

Check-in exige participação ativa na comunidade.

### RB-04

Check-in excluído deve possuir soft delete.

### RB-05

A exclusão de um check-in pode exigir reversão de pontos e atualização
das projeções relacionadas.

### RB-06

Somente papéis autorizados podem administrar membros e solicitações.

### RB-07

Refresh tokens devem ser rotacionados.

### RB-08

Webhooks devem ser idempotentes.

### RB-09

Operações potencialmente duplicáveis devem aceitar `Idempotency-Key`.

### RB-10

O backend é responsável pelas validações de negócio.

## 3. Requisitos não funcionais

### RNF-01 --- Segurança

-   HTTPS em produção;
-   autenticação robusta;
-   autorização contextual;
-   tokens de curta duração;
-   refresh token seguro;
-   secrets fora do código;
-   validação de uploads;
-   rate limiting.

### RNF-02 --- Escalabilidade

A aplicação deve iniciar como modular monolith, mas permitir extração
futura de módulos.

### RNF-03 --- Disponibilidade

Falhas de componentes secundários não devem necessariamente impedir
operações core.

### RNF-04 --- Consistência

PostgreSQL é a fonte de verdade.

Redis não será fonte de verdade.

### RNF-05 --- Observabilidade

Toda requisição relevante deve possuir:

-   logs estruturados;
-   traceId;
-   métricas;
-   informações suficientes para investigação.

### RNF-06 --- Testabilidade

O sistema deve possuir:

-   unit tests;
-   integration tests;
-   contract tests;
-   testes de autorização;
-   testes de regras críticas.

### RNF-07 --- Evolução

A API deve ser versionada.

Breaking changes não devem ser introduzidas silenciosamente.

### RNF-08 --- Performance

Consultas críticas devem possuir índices e paginação.

Feed e rankings devem estar preparados para evolução para
Redis/materializações.

### RNF-09 --- Privacidade

Dados pessoais devem possuir política de retenção e
exclusão/anonymização adequada.

### RNF-10 --- Manutenibilidade

A arquitetura deve manter separação entre:

``` text
Presentation
Application
Domain
Infrastructure
```
