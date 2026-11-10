-- Seed initial badges catalog
INSERT INTO badges (id, name, description, image_url, created_at)
VALUES 
    ('b1000000-0000-0000-0000-000000000001', 'Primeiro Check-in', 'Realizou o primeiro check-in de estudos na plataforma.', '/badges/first_checkin.png', CURRENT_TIMESTAMP),
    ('b1000000-0000-0000-0000-000000000002', '7 Dias Seguidos', 'Manteve consistência nos estudos por 7 dias consecutivos.', '/badges/streak_7.png', CURRENT_TIMESTAMP),
    ('b1000000-0000-0000-0000-000000000003', '30 Dias Seguidos', 'Manteve consistência nos estudos por 30 dias consecutivos.', '/badges/streak_30.png', CURRENT_TIMESTAMP),
    ('b1000000-0000-0000-0000-000000000004', 'Criador de Comunidade', 'Fundou uma comunidade de estudos para apoiar outros colegas.', '/badges/community_creator.png', CURRENT_TIMESTAMP),
    ('b1000000-0000-0000-0000-000000000005', '100 Pontos', 'Acumulou 100 pontos de dedicação nos estudos.', '/badges/points_100.png', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
