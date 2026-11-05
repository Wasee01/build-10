INSERT INTO students (id, name) VALUES (1, 'Alex Student'), (2, 'Jordan Student'), (3, 'Casey Student');
INSERT INTO sections (id, term_code, title, credit_hours, capacity, registration_opens_at, registration_closes_at, version)
VALUES (1, 'DEMO', 'Java Development', 3, 1, CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP + INTERVAL '7' DAY, 0),
       (2, 'DEMO', 'Database Design', 3, 20, CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP + INTERVAL '7' DAY, 0);
