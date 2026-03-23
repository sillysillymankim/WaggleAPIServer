create table waggle.application_reads
(
    id             bigint auto_increment
        primary key,
    created_at     datetime(6) not null,
    deleted_at     datetime(6) null,
    updated_at     datetime(6) not null,
    application_id bigint      not null,
    user_id        binary(16)  not null,
    constraint UK4pcjufqd0ac6004ilb2ejy1gg
        unique (application_id, user_id)
);

create index idx_application_reads_application
    on waggle.application_reads (application_id);

create index idx_application_reads_user
    on waggle.application_reads (user_id);

create table waggle.applications
(
    created_at datetime(6)   not null,
    deleted_at datetime(6)   null,
    id         bigint auto_increment
        primary key,
    updated_at datetime(6)   not null,
    user_id    binary(16)    not null,
    detail     varchar(5000) null,
    position   varchar(20)   not null,
    status     varchar(20)   not null,
    post_id    bigint        not null,
    team_id    bigint        not null,
    constraint UK7uplan8w2mdqesciciufjepsx
        unique (team_id, user_id, position)
);

create table waggle.application_portfolio_urls
(
    application_id bigint        not null,
    portfolio_url  varchar(2048) not null,
    constraint FKrketpvh5st69qrycymhxmvwvh
        foreign key (application_id) references waggle.applications (id)
);

create index idx_applications_post
    on waggle.applications (post_id);

create index idx_applications_team_status
    on waggle.applications (team_id, status);

create index idx_applications_user
    on waggle.applications (user_id);

create table waggle.bookmarks
(
    created_at datetime(6) not null,
    target_id  bigint      not null,
    user_id    binary(16)  not null,
    type       varchar(20) not null,
    primary key (target_id, user_id, type)
);

create index idx_bookmarks_type_target
    on waggle.bookmarks (type, target_id);

create table waggle.conversations
(
    id                   bigint auto_increment
        primary key,
    created_at           datetime(6) not null,
    deleted_at           datetime(6) null,
    updated_at           datetime(6) not null,
    last_message_id      bigint      not null,
    last_read_message_id bigint      null,
    partner_id           binary(16)  not null,
    unread_count         bigint      not null,
    user_id              binary(16)  not null,
    constraint uk_conversations_user_partner
        unique (user_id, partner_id)
);

create index idx_conversations_user_last_message
    on waggle.conversations (user_id asc, last_message_id desc);

create table waggle.follows
(
    created_at  datetime(6) not null,
    id          bigint auto_increment
        primary key,
    followee_id binary(16)  not null,
    follower_id binary(16)  not null,
    constraint UK53c7l7tclgps8jhvuioqbti7n
        unique (follower_id, followee_id)
);

create index idx_follows_followee
    on waggle.follows (followee_id);

create table waggle.invitations
(
    accepted_at    datetime(6) null,
    application_id bigint      null,
    created_at     datetime(6) not null,
    declined_at    datetime(6) null,
    deleted_at     datetime(6) null,
    expires_at     datetime(6) null,
    id             bigint auto_increment
        primary key,
    project_id     bigint      not null,
    updated_at     datetime(6) not null,
    user_id        bigint      not null,
    status         varchar(20) not null,
    team_id        bigint      not null
);

create table waggle.member_reviews
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6) not null,
    deleted_at  datetime(6) null,
    updated_at  datetime(6) not null,
    reviewee_id binary(16)  not null,
    reviewer_id binary(16)  not null,
    team_id     bigint      not null,
    type        varchar(10) not null,
    constraint uk_member_reviews_reviewer_reviewee_team
        unique (reviewer_id, reviewee_id, team_id)
);

create table waggle.member_review_tags
(
    member_review_id bigint      not null,
    tag              varchar(30) not null,
    primary key (member_review_id, tag),
    constraint FK9ewk4etp48oj5fl5ue8mw53bg
        foreign key (member_review_id) references waggle.member_reviews (id)
);

create index idx_member_reviews_reviewee
    on waggle.member_reviews (reviewee_id);

create index idx_member_reviews_reviewer_team
    on waggle.member_reviews (reviewer_id, team_id);

create table waggle.members
(
    created_at datetime(6) not null,
    deleted_at datetime(6) null,
    id         bigint auto_increment
        primary key,
    updated_at datetime(6) not null,
    user_id    binary(16)  not null,
    role       varchar(10) not null,
    team_id    bigint      not null,
    is_visible bit         not null,
    position   varchar(20) not null,
    deleted_by binary(16)  null,
    constraint UKbm6b0b3pp1dfasf3d9wyahi9i
        unique (user_id, team_id)
);

create index idx_members_team
    on waggle.members (team_id);

create table waggle.messages
(
    created_at  datetime(6) not null,
    id          bigint auto_increment
        primary key,
    read_at     datetime(6) null,
    receiver_id binary(16)  not null,
    sender_id   binary(16)  not null,
    content     text        not null,
    deleted_at  datetime(6) null,
    updated_at  datetime(6) not null
);

create index idx_messages_receiver_read
    on waggle.messages (receiver_id, read_at);

create index idx_messages_receiver_sender_created
    on waggle.messages (receiver_id, sender_id, created_at);

create index idx_messages_sender_receiver_created
    on waggle.messages (sender_id, receiver_id, created_at);

create table waggle.notifications
(
    created_at   datetime(6) not null,
    id           bigint auto_increment
        primary key,
    read_at      datetime(6) null,
    user_id      binary(16)  not null,
    type         varchar(32) not null,
    team_id      bigint      null,
    triggered_by binary(16)  null
);

create index idx_notifications_user_read_created
    on waggle.notifications (user_id asc, read_at asc, created_at desc);

create table waggle.posts
(
    created_at datetime(6)   not null,
    deleted_at datetime(6)   null,
    id         bigint auto_increment
        primary key,
    updated_at datetime(6)   not null,
    user_id    binary(16)    not null,
    content    varchar(5000) not null,
    title      varchar(255)  not null,
    team_id    bigint        not null
);

create index idx_posts_title
    on waggle.posts (title);

create table waggle.recruitments
(
    created_at datetime(6) not null,
    deleted_at datetime(6) null,
    id         bigint auto_increment
        primary key,
    updated_at datetime(6) not null,
    position   varchar(20) not null,
    post_id    bigint      not null,
    status     varchar(20) not null,
    count      int         not null,
    constraint UKmu3h68kwbfikd3htxkr0ug0rc
        unique (post_id, position)
);

create table waggle.recruitment_skills
(
    recruitment_id bigint      not null,
    skill          varchar(30) not null,
    primary key (recruitment_id, skill),
    constraint FKfi7fj762k809ttm5n0hee5oft
        foreign key (recruitment_id) references waggle.recruitments (id)
);

create index idx_recruitments_post
    on waggle.recruitments (post_id);

create table waggle.teams
(
    id                bigint auto_increment
        primary key,
    created_at        datetime(6)  not null,
    deleted_at        datetime(6)  null,
    updated_at        datetime(6)  not null,
    creator_id        binary(16)   not null,
    description       text         not null,
    leader_id         binary(16)   not null,
    name              varchar(255) not null,
    profile_image_url varchar(255) null,
    work_mode         varchar(20)  not null,
    status            varchar(20)  not null,
    constraint UKa510no6sjwqcx153yd5sm4jrr
        unique (name)
);

create index idx_teams_name
    on waggle.teams (name);

create table waggle.users
(
    created_at        datetime(6)         not null,
    deleted_at        datetime(6)         null,
    updated_at        datetime(6)         not null,
    id                binary(16)          not null
        primary key,
    bio               varchar(1000)       null,
    email             varchar(255)        not null,
    profile_image_url varchar(255)        null,
    provider          varchar(255)        not null,
    provider_id       varchar(255)        not null,
    username          varchar(255)        null,
    position          varchar(20)         null,
    role              varchar(10)         not null,
    temperature       double default 36.5 not null,
    constraint UKcbysvpk95086ud4n4g6mkspai
        unique (provider, provider_id)
);

create table waggle.user_portfolios
(
    user_id       binary(16)    not null,
    portfolio_url varchar(2048) not null,
    constraint FK1y44v9x6f4ts23ovfpxi0qnqm
        foreign key (user_id) references waggle.users (id)
);

create table waggle.user_skills
(
    user_id binary(16)  not null,
    skill   varchar(50) not null,
    primary key (user_id, skill),
    constraint FKro13if9r7fwkr5115715127ai
        foreign key (user_id) references waggle.users (id)
);

create index idx_users_email
    on waggle.users (email);

