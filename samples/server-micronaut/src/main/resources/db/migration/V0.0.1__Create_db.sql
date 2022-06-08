create table audit_trail
(
    id             uuid         not null primary key,
    transaction_id uuid         not null,
    action         varchar(255) not null,
    type           varchar(255) not null,
    target         uuid,
    reason         varchar(255),
    timestamp      timestamp    not null,
    epoch          bigint       not null,
    payload        jsonb,
    by             uuid         not null
);

create table if not exists organization
(
    id              uuid   not null primary key,
    created_at      timestamp,
    created_by      jsonb default '{}',
    last_updated_at timestamp,
    last_updated_by jsonb default '{}',
    version         bigint not null,
    metadata        jsonb default '{}',

    name            varchar(255)
);

insert into organization
values ('3fedcab7-97b7-4f81-b49f-2a70864f7cfa', now(), '{"id": "d568705a-a581-4923-828b-0f97c0891163"}', now(), '{}', 0, '{}', 'Org A'),
       ('d79d05fa-d670-44c2-91f1-866bd8798995', now(), '{}', now(), '{}', 0, '{}', 'Org B');

create table if not exists address
(
    id              uuid         not null
        primary key,
    created_at      timestamp,
    created_by      jsonb default '{}',
    last_updated_at timestamp,
    last_updated_by jsonb default '{}',
    version         bigint       not null,
    metadata        jsonb default '{}',

    organization_id uuid
        constraint fk_address_organization_id_organization references organization,
    line1           varchar(255) not null,
    line2           varchar(255),
    city            varchar(255) not null,
    postal_code     varchar(255) not null,
    country         varchar(2)   not null
);
insert into address
values ('02068a02-1072-44a8-ac66-d6e378628ef1', now(), '{}', now(), '{}', 0, '{}',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa',
        'Org A line 1', null, 'City A', 'AAAA-AAA', 'AA'),

       ('5c0d9686-9f26-4d61-880e-47f99a2dbf03', now(), '{}', now(), '{}', 0, '{}',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa',
        'A Senior Engineer line 1', null, 'City A', 'AAAA-AAA', 'AA'),
       ('f7963915-a5b4-4214-91f1-b844c9d51b21', now(), '{}', now(), '{}', 0, '{}',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa',
        'A Junior Engineer line 1', null, 'City A', 'AAAA-AAA', 'AA'),
       ('98859cd5-2963-4657-a98e-efab9a181cf0', now(), '{}', now(), '{}', 0, '{}',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa',
        'A Senior HR line 1', null, 'City A', 'AAAA-AAA', 'AA'),
       ('5f07f454-f811-45cc-ac77-c4ca57073304', now(), '{}', now(), '{}', 0, '{}',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa',
        'A Junior HR line 1', null, 'City A', 'AAAA-AAA', 'AA'),


       ('ef2255a7-7426-49cd-b2f4-7990141450ec', now(), '{}', now(), '{}', 0, '{}',
        'd79d05fa-d670-44c2-91f1-866bd8798995',
        'Org B line 1', null, 'City B', 'BBBB-BBB', 'BB'),

       ('0bb93356-ad24-4b7b-9dd4-2ca71b328650', now(), '{}', now(), '{}', 0, '{}',
        'd79d05fa-d670-44c2-91f1-866bd8798995',
        'B Senior Engineer line 1', null, 'City B', 'BBBB-BBB', 'BB'),
       ('2438c6ec-34ed-4638-a31f-5818e3a7ec23', now(), '{}', now(), '{}', 0, '{}',
        'd79d05fa-d670-44c2-91f1-866bd8798995',
        'B Junior Engineer line 1', null, 'City B', 'BBBB-BBB', 'BB'),
       ('6b259fa3-da17-4cb8-bff8-f2bd85336c83', now(), '{}', now(), '{}', 0, '{}',
        'd79d05fa-d670-44c2-91f1-866bd8798995',
        'B Senior HR line 1', null, 'City B', 'BBBB-BBB', 'BB'),
       ('0e3aa568-1b25-4d4e-9be0-3b0e23368ff4', now(), '{}', now(), '{}', 0, '{}',
        'd79d05fa-d670-44c2-91f1-866bd8798995',
        'B Junior HR line 1', null, 'City B', 'BBBB-BBB', 'BB');

create table if not exists billing
(
    id              uuid         not null
        primary key,
    created_at      timestamp,
    created_by      jsonb default '{}',
    last_updated_at timestamp,
    last_updated_by jsonb default '{}',
    version         bigint       not null,
    metadata        jsonb default '{}',

    organization_id uuid
        constraint fk_billing_organization_id_organization references organization,
    company_name    varchar(255) not null,
    vat_number      varchar(255) not null,
    address_id      uuid
        constraint fk_billing_address_id_address references address
);
insert into billing
values ('5ea8fa88-8bb1-4bab-bc4a-d38f358fac9d', now(), '{}', now(), '{}', 0, '{}',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa',
        'Oganization A LTD', 'AA-VATNUM',
        '02068a02-1072-44a8-ac66-d6e378628ef1'),
       ('55e9438c-8295-4d6e-b0a4-6b1d37aaf2a3', now(), '{}', now(), '{}', 0, '{}',
        'd79d05fa-d670-44c2-91f1-866bd8798995',
        'Oganization B LTD', 'BB-VATNUM',
        'ef2255a7-7426-49cd-b2f4-7990141450ec');

create table if not exists department
(
    id              uuid   not null
        primary key,
    created_at      timestamp,
    created_by      jsonb default '{}',
    last_updated_at timestamp,
    last_updated_by jsonb default '{}',
    version         bigint not null,
    metadata        jsonb default '{}',

    name            varchar(255),
    parent_id       uuid
        constraint fk_department_parent_id_department references department,
    organization_id uuid
        constraint fk_department_organization_id_organization references organization
);

insert into department
values ('010958ae-75b5-4e90-b161-58420a3820db', now(), '{}', now(), '{}', 0, '{}', 'A Departments', null,
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa'),
       ('a3a9d141-5614-4686-821e-f3c3e3ace530', now(), '{}', now(), '{}', 0, '{}', 'A Engineering',
        '010958ae-75b5-4e90-b161-58420a3820db',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa'),
       ('7217d08f-09a4-4ef7-b978-48a4e1c5079a', now(), '{}', now(), '{}', 0, '{}', 'A HR',
        '010958ae-75b5-4e90-b161-58420a3820db',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa'),
       ('3b5e2695-b8e9-4b76-afb1-28f98c66f3ad', now(), '{}', now(), '{}', 0, '{}', 'B Departments', null,
        'd79d05fa-d670-44c2-91f1-866bd8798995'),
       ('b614e2f2-bf91-4b78-a39f-48f3ca334f2f', now(), '{}', now(), '{}', 0, '{}', 'B Engineering',
        '3b5e2695-b8e9-4b76-afb1-28f98c66f3ad',
        'd79d05fa-d670-44c2-91f1-866bd8798995'),
       ('bee94a58-ec0c-48c9-a36d-60a55673b7f8', now(), '{}', now(), '{}', 0, '{}', 'B HR',
        '3b5e2695-b8e9-4b76-afb1-28f98c66f3ad',
        'd79d05fa-d670-44c2-91f1-866bd8798995');


create table if not exists employee
(
    id              uuid    not null
        primary key,
    created_at      timestamp,
    created_by      jsonb default '{}',
    last_updated_at timestamp,
    last_updated_by jsonb default '{}',
    version         bigint  not null,
    metadata        jsonb default '{}',

    age             integer not null,
    name            varchar(255),
    position        varchar(255),
    salary          integer not null,
    address_id      uuid
        constraint fk_employee_address_id_address references address,
    department_id   uuid
        constraint fk_employee_department_id_department references department,
    organization_id uuid
        constraint fk_employee_organization_id_organization references organization
);

insert into employee
values ('e9fa40a7-3044-4328-82e9-f710a0911452', now(), '{}', now(), '{}', 0, '{}', 35, 'Sen Eng', 'A Senior Engineer',
        100000,
        '5c0d9686-9f26-4d61-880e-47f99a2dbf03', 'a3a9d141-5614-4686-821e-f3c3e3ace530',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa'),
       ('f0203a8f-cb3b-430b-a866-cdf7ea1ed730', now(), '{}', now(), '{}', 0, '{}', 25, 'Jun Eng', 'A Junior Engineer',
        50000,
        'f7963915-a5b4-4214-91f1-b844c9d51b21', 'a3a9d141-5614-4686-821e-f3c3e3ace530',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa'),
       ('f33ebe50-7fe6-42d0-b7c4-56848c93607d', now(), '{}', now(), '{}', 0, '{}', 25, 'Sen Har', 'A Senior HR', 100000,
        '98859cd5-2963-4657-a98e-efab9a181cf0', '7217d08f-09a4-4ef7-b978-48a4e1c5079a',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa'),
       ('b1b41793-cb41-4036-a3f8-93a06b219fea', now(), '{}', now(), '{}', 0, '{}', 25, 'Jun Har', 'A Junior HR', 50000,
        '5f07f454-f811-45cc-ac77-c4ca57073304', '7217d08f-09a4-4ef7-b978-48a4e1c5079a',
        '3fedcab7-97b7-4f81-b49f-2a70864f7cfa'),

       ('b603046e-9f5e-40ac-b06b-0cede475b0ea', now(), '{}', now(), '{}', 0, '{}', 35, 'Sen Eng', 'B Senior Engineer',
        100000,
        '0bb93356-ad24-4b7b-9dd4-2ca71b328650', 'b614e2f2-bf91-4b78-a39f-48f3ca334f2f',
        'd79d05fa-d670-44c2-91f1-866bd8798995'),
       ('0d7cb6b2-7e67-4aec-b165-41532a09a53b', now(), '{}', now(), '{}', 0, '{}', 25, 'Jun Eng', 'B Junior Engineer',
        50000,
        '2438c6ec-34ed-4638-a31f-5818e3a7ec23', 'b614e2f2-bf91-4b78-a39f-48f3ca334f2f',
        'd79d05fa-d670-44c2-91f1-866bd8798995'),
       ('6b1df6c0-f135-4ec9-9f25-bb0238041173', now(), '{}', now(), '{}', 0, '{}', 25, 'Sen Har', 'B Senior HR', 100000,
        '6b259fa3-da17-4cb8-bff8-f2bd85336c83', 'bee94a58-ec0c-48c9-a36d-60a55673b7f8',
        'd79d05fa-d670-44c2-91f1-866bd8798995'),
       ('c2867314-2bee-49d7-ac1a-8307d6cd1790', now(), '{}', now(), '{}', 0, '{}', 25, 'Jun Har', 'B Junior HR', 50000,
        '0e3aa568-1b25-4d4e-9be0-3b0e23368ff4', 'bee94a58-ec0c-48c9-a36d-60a55673b7f8',
        'd79d05fa-d670-44c2-91f1-866bd8798995');
