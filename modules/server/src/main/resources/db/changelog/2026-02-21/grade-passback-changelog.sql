--liquibase formatted sql

--changeset Dmitry Smirnov:2026-02-21-01-create-attempt-lti-context
create table attempt_lti_context
(
    id           bigint       not null primary key,
    lineitem_url varchar(512) not null,
    context_id   varchar(255) null,
    constraint fk_attempt_lti_context_attempt
        foreign key (id) references exercise_attempt (id) on delete cascade
);
--rollback drop table attempt_lti_context;

--changeset Dmitry Smirnov:2026-02-21-02-create-education-resource
create table education_resource
(
    id         bigint auto_increment primary key,
    url        varchar(512) not null,
    type       varchar(64)  not null,
    created_at datetime     not null default current_timestamp,
    updated_at datetime     not null default current_timestamp on update current_timestamp,
    constraint uq_education_resource_url_type unique (url, type)
);
--rollback drop table education_resource;

--changeset Dmitry Smirnov:2026-02-21-03-create-moodle-education-resource
create table moodle_education_resource
(
    id bigint not null primary key,
    constraint fk_moodle_education_resource_parent
        foreign key (id) references education_resource (id) on delete cascade
);
--rollback drop table moodle_education_resource;

--changeset Dmitry Smirnov:2026-02-21-04-create-external-account
create table external_account
(
    id                    bigint auto_increment primary key,
    user_id               bigint      not null,
    education_resource_id bigint      not null,
    type                  varchar(32) not null,
    created_at            datetime    not null default current_timestamp,
    updated_at            datetime    not null default current_timestamp on update current_timestamp,
    constraint fk_external_account_user
        foreign key (user_id) references user (id),
    constraint fk_external_account_education_resource
        foreign key (education_resource_id) references education_resource (id),
    constraint uq_external_account_user_resource unique (user_id, education_resource_id)
);
--rollback drop table external_account;

--changeset Dmitry Smirnov:2026-02-21-05-create-moodle-account
create table moodle_account
(
    id             bigint not null primary key,
    moodle_user_id bigint not null,
    constraint fk_moodle_account_parent
        foreign key (id) references external_account (id) on delete cascade
);
--rollback drop table moodle_account;
