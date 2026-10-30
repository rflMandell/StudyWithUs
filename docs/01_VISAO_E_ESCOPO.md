# StudyWithUs --- Visão e Escopo do Produto

## 1. Visão

O StudyWithUs é uma plataforma social de motivação acadêmica que busca
ajudar estudantes a desenvolver consistência nos estudos e na
participação em aulas por meio de comunidades, desafios, check-ins,
pontos e rankings.

A proposta central é transformar pequenas ações acadêmicas em progresso
visível e coletivo.

## 2. Problema

Estudantes podem enfrentar:

-   falta de motivação;
-   dificuldade de manter consistência;
-   sensação de estudar sozinhos;
-   ausência de acompanhamento do próprio progresso;
-   falta de mecanismos de reconhecimento social.

O produto busca atacar esse problema por meio de um ciclo simples:

``` text
Estudar / participar de aula
        ↓
Registrar check-in
        ↓
Ganhar pontos
        ↓
Progredir
        ↓
Aparecer no ranking
        ↓
Receber reconhecimento
        ↓
Continuar estudando
```

## 3. Público-alvo

A plataforma deve atender diferentes perfis:

-   estudantes universitários;
-   ensino médio;
-   ensino fundamental;
-   vestibulandos;
-   concurseiros;
-   estudantes autônomos.

## 4. Princípios do produto

### Consistência acima de quantidade

O produto deve valorizar regularidade e realização de atividades, e não
apenas quantidade de horas estudadas.

### Competição saudável

Rankings e pontos devem funcionar como mecanismos de motivação, sem
transformar o produto em uma competição prejudicial.

### Comunidade como apoio

A experiência social deve facilitar acompanhamento e incentivo entre
estudantes.

### Acesso amplo

O produto deve atender diferentes níveis e objetivos acadêmicos.

### Simplicidade

Registrar uma atividade deve ser rápido e claro.

## 5. Loop principal de engajamento

``` text
Usuário entra
   ↓
Visualiza progresso
   ↓
Faz check-in
   ↓
Recebe pontos
   ↓
Ranking é atualizado
   ↓
Atividade aparece no feed
   ↓
Usuário acompanha progresso
   ↓
Volta a estudar
```

## 6. Funcionalidades principais

### Autenticação

-   cadastro;
-   login;
-   logout;
-   refresh token;
-   Google OAuth;
-   perfil.

### Comunidades

-   criação;
-   exploração;
-   comunidades públicas;
-   comunidades privadas;
-   entrada;
-   saída;
-   membros;
-   papéis administrativos;
-   solicitações de entrada;
-   ranking configurável.

### Check-ins

Tipos conceituais:

-   presença em aula;
-   momento de estudo.

O usuário pode selecionar uma atividade ou fornecer uma descrição
personalizada.

### Pontuação

Check-ins geram pontos.

Exemplos históricos de pontuação:

-   aula: 50;
-   estudo: 30;
-   meta diária: 20;
-   desafio: 100.

Na arquitetura atual, regras de pontuação devem ser tratadas como regra
de negócio/configuração, não como valores espalhados por controllers.

### Streaks

Sequência de dias ativos.

Exemplo:

``` text
7 dias consecutivos
```

O cálculo deve respeitar o conceito de dia da comunidade/usuário e sua
timezone.

### Rankings

Períodos:

-   semanal;
-   semestral;
-   anual;
-   permanente.

A arquitetura atual também suporta estrutura para períodos e
configuração de ranking.

### Feed

O feed apresenta atividades de membros das comunidades e pessoas
relacionadas ao usuário.

Inicialmente, check-ins são a publicação principal.

### Desafios

Possíveis métricas:

-   quantidade de atividades;
-   dias consecutivos;
-   metas concluídas;
-   pontos.

Escopos:

-   individual;
-   coletivo.

### Perfil

O usuário pode visualizar:

-   pontos;
-   streak;
-   check-ins;
-   comunidades;
-   badges;
-   ranking contextual.

## 7. Telas principais

``` text
Login / Registro
Home
Comunidades
Explorar comunidades
Detalhes da comunidade
Feed
Ranking
Check-in
Perfil
Desafios
Notificações
Configurações
```

## 8. MVP

O MVP conceitual prioriza:

-   autenticação;
-   comunidades;
-   entrada em comunidades;
-   check-ins;
-   pontos;
-   leaderboard;
-   perfil;
-   streak;
-   feed.

## 9. Fora do MVP / evolução

Itens planejados para evolução:

-   IA / AI Coach;
-   analytics avançado;
-   upload de imagens;
-   temas personalizados;
-   regras avançadas de pontuação;
-   criação de mais comunidades via Premium;
-   ligas;
-   níveis;
-   aplicativo mobile;
-   recursos sociais adicionais.

## 10. Monetização

Modelo:

``` text
Freemium
```

Funcionalidades premium planejadas:

-   mais comunidades;
-   personalização visual;
-   personalização de pontos;
-   recursos avançados;
-   AI Coach futuramente.

A arquitetura atual já prevê `subscriptions` e `subscription_events`
para suportar essa evolução.

## 11. Métrica central do produto

Uma métrica coerente com a proposta é a consistência de atividade:

``` text
usuários ativos
+
dias ativos
+
check-ins válidos
+
retenção
```

O produto não deve tratar simplesmente "tempo estudado" como única
medida de sucesso.
