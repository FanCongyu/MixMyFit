create table users (
    user_id bigint not null auto_increment,
    username varchar(100) not null,
    password_hash varchar(255) not null,
    nickname varchar(100) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    primary key (user_id),
    constraint uk_users_username unique (username)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table categories (
    category_id bigint not null auto_increment,
    user_id bigint null,
    name varchar(100) not null,
    type varchar(20) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    primary key (category_id),
    constraint fk_categories_user foreign key (user_id) references users (user_id) on delete cascade,
    constraint uk_categories_user_name unique (user_id, name),
    constraint chk_categories_type check (type in ('fixed', 'custom')),
    constraint chk_categories_owner_matches_type check (
        (type = 'fixed' and user_id is null)
        or (type = 'custom' and user_id is not null)
    )
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table clothing_tags (
    clothing_tag_id bigint not null auto_increment,
    user_id bigint not null,
    name varchar(100) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    primary key (clothing_tag_id),
    constraint fk_clothing_tags_user foreign key (user_id) references users (user_id) on delete cascade,
    constraint uk_clothing_tags_user_name unique (user_id, name)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table outfit_tags (
    outfit_tag_id bigint not null auto_increment,
    user_id bigint not null,
    name varchar(100) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    primary key (outfit_tag_id),
    constraint fk_outfit_tags_user foreign key (user_id) references users (user_id) on delete cascade,
    constraint uk_outfit_tags_user_name unique (user_id, name)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table clothes (
    clothing_id bigint not null auto_increment,
    user_id bigint not null,
    category_id bigint null,
    name varchar(120) null,
    color varchar(80) null,
    image_path varchar(500) not null,
    original_filename varchar(255) not null,
    content_type varchar(100) not null,
    file_size bigint not null,
    status varchar(20) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    primary key (clothing_id),
    constraint fk_clothes_user foreign key (user_id) references users (user_id) on delete cascade,
    constraint fk_clothes_category foreign key (category_id) references categories (category_id) on delete restrict,
    constraint uq_clothes_clothing_user unique (clothing_id, user_id),
    constraint chk_clothes_file_size check (file_size >= 0),
    constraint chk_clothes_status check (status in ('draft', 'ready')),
    constraint chk_clothes_status_matches_category check (
        (status = 'draft' and category_id is null)
        or (status = 'ready' and category_id is not null)
    )
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table clothing_seasons (
    clothing_season_id bigint not null auto_increment,
    clothing_id bigint not null,
    season varchar(20) not null,
    primary key (clothing_season_id),
    constraint fk_clothing_seasons_clothing foreign key (clothing_id) references clothes (clothing_id) on delete cascade,
    constraint uk_clothing_seasons_clothing_season unique (clothing_id, season),
    constraint chk_clothing_seasons_season check (season in ('spring', 'summer', 'autumn', 'winter'))
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table clothing_tag_links (
    clothing_tag_link_id bigint not null auto_increment,
    clothing_id bigint not null,
    clothing_tag_id bigint not null,
    primary key (clothing_tag_link_id),
    constraint fk_clothing_tag_links_clothing foreign key (clothing_id) references clothes (clothing_id) on delete cascade,
    constraint fk_clothing_tag_links_tag foreign key (clothing_tag_id) references clothing_tags (clothing_tag_id) on delete cascade,
    constraint uk_clothing_tag_links_pair unique (clothing_id, clothing_tag_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table outfits (
    outfit_id bigint not null auto_increment,
    user_id bigint not null,
    title varchar(160) null,
    note text null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    primary key (outfit_id),
    constraint fk_outfits_user foreign key (user_id) references users (user_id) on delete cascade,
    constraint uq_outfits_outfit_user unique (outfit_id, user_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table outfit_seasons (
    outfit_season_id bigint not null auto_increment,
    outfit_id bigint not null,
    season varchar(20) not null,
    primary key (outfit_season_id),
    constraint fk_outfit_seasons_outfit foreign key (outfit_id) references outfits (outfit_id) on delete cascade,
    constraint uk_outfit_seasons_outfit_season unique (outfit_id, season),
    constraint chk_outfit_seasons_season check (season in ('spring', 'summer', 'autumn', 'winter'))
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table outfit_tag_links (
    outfit_tag_link_id bigint not null auto_increment,
    outfit_id bigint not null,
    outfit_tag_id bigint not null,
    primary key (outfit_tag_link_id),
    constraint fk_outfit_tag_links_outfit foreign key (outfit_id) references outfits (outfit_id) on delete cascade,
    constraint fk_outfit_tag_links_tag foreign key (outfit_tag_id) references outfit_tags (outfit_tag_id) on delete cascade,
    constraint uk_outfit_tag_links_pair unique (outfit_id, outfit_tag_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table outfit_items (
    outfit_item_id bigint not null auto_increment,
    outfit_id bigint not null,
    user_id bigint not null,
    clothing_id bigint not null,
    role varchar(30) not null,
    slot varchar(20) null,
    position_x int null,
    position_y int null,
    size varchar(20) null,
    z_index int null,
    primary key (outfit_item_id),
    constraint fk_outfit_items_outfit_owner foreign key (outfit_id, user_id) references outfits (outfit_id, user_id) on delete cascade,
    constraint fk_outfit_items_clothing_owner foreign key (clothing_id, user_id) references clothes (clothing_id, user_id) on delete restrict,
    constraint chk_outfit_items_role check (role in ('main_slot', 'accessory_overlay')),
    constraint chk_outfit_items_slot check (slot is null or slot in ('top', 'bottom', 'shoes', 'hat')),
    constraint chk_outfit_items_size check (size is null or size in ('small', 'medium', 'large')),
    constraint chk_outfit_items_role_fields check (
        (
            role = 'main_slot'
            and slot is not null
            and position_x is null
            and position_y is null
            and size is null
            and z_index is null
        )
        or (
            role = 'accessory_overlay'
            and slot is null
            and position_x is not null
            and position_y is not null
            and size is not null
            and z_index is not null
        )
    )
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
