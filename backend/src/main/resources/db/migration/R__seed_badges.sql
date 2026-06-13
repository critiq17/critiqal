INSERT INTO badges (code, name, description, config) VALUES
    ('ORIGIN',    'Origin',
     'One of the first members of Critiqal',
     '{"max_members": 100}'),

    ('CENTURION', 'Centurion',
     '100 days on Critiqal',
     '{"min_days": 100}'),

    ('GLADIATOR', 'Gladiator',
     'One year of active presence on Critiqal',
     '{"min_days": 365}'),

    ('LEGATUS',   'Legatus',
     'Two years on Critiqal',
     '{"min_days": 730}'),

    ('SCRIBE',    'Scribe',
     'Published first post',
     '{"trigger": "first_post"}'),

    ('ORATOR',    'Orator',
     'Published 50 posts',
     '{"min_posts": 50}'),

    ('TRIBUNE',   'Tribune',
     'Active contributor to discussions',
     '{"min_posts": 500, "min_comments": 100, "min_likes": 1000}')

ON CONFLICT (code) DO UPDATE
    SET name        = EXCLUDED.name,
        description = EXCLUDED.description;
        config      = EXCLUDED.config;