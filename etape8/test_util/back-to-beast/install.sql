-- INSCRYPCARDS - Installation complète (ALL IN ONE)
-- ATTENTION : supprime toutes les tables existantes (et leurs données) puis recrée la base avec cartes et boutique.
-- Généré à partir de schema.sql + seeds/cards.sql + seeds/seed.sql.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE inscrynadmin;
DROP TABLE IF EXISTS games, friendships, user_avatar_parts, avatar_parts, shop_packs, deck_cards, decks, user_cards, cards, login_attempts, users;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== SCHEMA =====
-- INSCRYPCARDS - Schéma de base de données
-- Sélectionner la base cible avant l'import (USE nom_base; ou phpMyAdmin).

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    id              INT UNSIGNED NOT NULL AUTO_INCREMENT,
    username        VARCHAR(20)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    avatar          VARCHAR(100) NOT NULL DEFAULT 'default.png',
    avatar_top      TINYINT UNSIGNED NULL DEFAULT 1,
    avatar_face     TINYINT UNSIGNED NULL DEFAULT 1,
    avatar_bottom   TINYINT UNSIGNED NULL DEFAULT 1,
    level           SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    xp              INT UNSIGNED NOT NULL DEFAULT 0,
    coins           INT UNSIGNED NOT NULL DEFAULT 500,
    games_played    INT UNSIGNED NOT NULL DEFAULT 0,
    games_won       INT UNSIGNED NOT NULL DEFAULT 0,
    rank_tier       ENUM('novice','apprentice','journeyman','kaycee','scrybe') NOT NULL DEFAULT 'novice',
    rank_points     INT UNSIGNED NOT NULL DEFAULT 1000,
    starter_scribe  ENUM('leshy','grimora','p03','magnificus') NULL DEFAULT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at   TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS login_attempts (
    id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
    ip           VARCHAR(45) NOT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_login_attempts_ip (ip, attempted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cards (
    id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
    slug        VARCHAR(50)  NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    tribe       VARCHAR(100) NOT NULL DEFAULT 'none',
    scribe      ENUM('neutral','leshy','grimora','p03','magnificus') NOT NULL DEFAULT 'neutral',
    rarity      ENUM('common','uncommon','rare','ground') NOT NULL DEFAULT 'common',
    cost_type   ENUM('none','blood','bones','energy','bluegem','greengem','orangegem') NOT NULL DEFAULT 'none',
    cost_amount TINYINT UNSIGNED NOT NULL DEFAULT 0,
    attack      TINYINT UNSIGNED NOT NULL DEFAULT 0,
    health      TINYINT UNSIGNED NOT NULL DEFAULT 1,
    sigils      VARCHAR(255) NOT NULL DEFAULT '',
    special_ability VARCHAR(100) NOT NULL DEFAULT '',
    is_obtenable TINYINT(1) NOT NULL DEFAULT 1,
    repainted   TINYINT(1) NOT NULL DEFAULT 0,
    is_repainted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_cards_slug (slug),
    KEY idx_cards_scribe (scribe),
    KEY idx_cards_rarity (rarity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_cards (
    user_id  INT UNSIGNED NOT NULL,
    card_id  INT UNSIGNED NOT NULL,
    quantity SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    has_repaint TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, card_id),
    CONSTRAINT fk_user_cards_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_cards_card FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS decks (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    INT UNSIGNED NOT NULL,
    name       VARCHAR(30)  NOT NULL,
    is_active  TINYINT(1)   NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_decks_user (user_id),
    CONSTRAINT fk_decks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS deck_cards (
    deck_id  INT UNSIGNED NOT NULL,
    card_id  INT UNSIGNED NOT NULL,
    quantity SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    use_repaint TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (deck_id, card_id),
    CONSTRAINT fk_deck_cards_deck FOREIGN KEY (deck_id) REFERENCES decks (id) ON DELETE CASCADE,
    CONSTRAINT fk_deck_cards_card FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shop_packs (
    id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
    slug        VARCHAR(30)  NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(150) NOT NULL DEFAULT '',
    image       VARCHAR(60)  NOT NULL,
    price       INT UNSIGNED NOT NULL,
    card_count  TINYINT UNSIGNED NOT NULL,
    kind        ENUM('scribe','repaint','face') NOT NULL DEFAULT 'scribe',
    scribe      ENUM('leshy','grimora','p03','magnificus') NULL DEFAULT NULL,
    sort_order  TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_shop_packs_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS avatar_parts (
    part    ENUM('top','face','bottom') NOT NULL,
    variant TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (part, variant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO avatar_parts (part, variant) VALUES
('top',1),('top',2),('top',3),('top',4),('top',5),('top',6),
('face',1),('face',2),('face',3),('face',4),('face',5),('face',6),('face',7),
('bottom',1),('bottom',2),('bottom',3),('bottom',4),('bottom',5),('bottom',6);

-- Parties d'avatar débloquées par joueur
CREATE TABLE IF NOT EXISTS user_avatar_parts (
    user_id INT UNSIGNED NOT NULL,
    part    ENUM('top','face','bottom') NOT NULL,
    variant TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id, part, variant),
    CONSTRAINT fk_avatar_parts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_avatar_parts_catalog FOREIGN KEY (part, variant) REFERENCES avatar_parts (part, variant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS friendships (
    id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
    requester_id INT UNSIGNED NOT NULL,
    addressee_id INT UNSIGNED NOT NULL,
    status       ENUM('pending','accepted') NOT NULL DEFAULT 'pending',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_friendships_pair (requester_id, addressee_id),
    KEY idx_friendships_addressee (addressee_id, status),
    CONSTRAINT fk_friendships_requester FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_friendships_addressee FOREIGN KEY (addressee_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS games (
    id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
    player1_id  INT UNSIGNED NOT NULL,
    player2_id  INT UNSIGNED NOT NULL,
    winner_id   INT UNSIGNED NULL DEFAULT NULL,
    mode        ENUM('casual','ranked') NOT NULL DEFAULT 'casual',
    played_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_games_player1 (player1_id, played_at),
    KEY idx_games_player2 (player2_id, played_at),
    CONSTRAINT fk_games_player1 FOREIGN KEY (player1_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_games_player2 FOREIGN KEY (player2_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_games_winner  FOREIGN KEY (winner_id)  REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===== CARTES =====
-- only purpose is to fill up all the cards

-- leshy
INSERT INTO cards (slug, name, tribe, scribe, rarity, cost_type, cost_amount, attack, health, sigils, special_ability, is_obtenable, repainted, is_repainted) VALUES
-- squirrels
('squirrel','Squirrel','squirrel','neutral','common','none',0, 0, 1, '','',0,1,0),
-- bird
('kingfisher','Kingfisher','bird','leshy','common','blood',1, 1, 1, 'flying,submerge', '',1,1,0),
('ravenegg','Raven Egg', 'bird','leshy','common','blood',1, 0, 2, 'evolve:raven', '',1,1,0),
('sparrow','Sparrow', 'bird','leshy','common','blood',1, 1, 2, 'flying', '',1,1,0),
('magpie', 'Magpie', 'bird','leshy','uncommon','blood',2, 1, 1, 'flying, tutor', '',1,1,0),
('raven', 'Raven', 'bird','leshy','uncommon','blood',2, 2, 3, 'flying', '',1,1,0),
('vulture', 'Vulture', 'bird','leshy','uncommon','bones',8, 3, 3, 'flying', '',1,1,0),
-- canine
('stuntedwolf', 'Stunted Wolf', 'canine','leshy','common','blood',1, 2, 2, '', '',1,1,0),
('wolfcub', 'Wolf Cub', 'canine','leshy','common','blood',1, 1, 1, 'evolve:wolf', '',1,1,0),
('bloodhound', 'Bloodhound', 'canine','leshy','uncommon','blood',2, 2, 3, 'guarddog', '',1,1,0),
('cagedwolf', 'Caged Wolf', 'canine','leshy','ground','blood',2, 0, 6, '', 'cage:wolf',1,1,0),
('wolf', 'Wolf', 'canine','leshy','uncommon','blood',2, 3, 2, '', '',1,1,0),
('coyote', 'Coyote', 'canine','leshy','uncommon','bones',4, 2, 1, '', '',1,1,0),
('alpha', 'Alpha', 'canine','leshy','uncommon','bones',4, 1, 2, 'buffneighbours', '',1,1,0),
-- hooved
-- reprendre la recherche des images après le Jersey Devil
('jerseydevil_sleeping', 'Child 13', 'hooved','leshy','rare','blood',1, 0, 1, 'sacrificial', 'hungrychild',1,1,0),
('decal_snelk_main', 'Long Elk', 'hooved','leshy','rare','bones',4, 1, 2, 'deathtouch,strafe', 'deposit:vertebrae',1,1,0),
('goat', 'Black Goat', 'hooved','leshy','common','blood',1, 0, 1, 'tripleblood', 'bonelord',1,1,0),
('deercub', 'Deer Cub', 'hooved','leshy','common','blood',1, 1, 1, 'evolve:elk,strafe', '',1,1,0),
('deer', 'Deer', 'hooved','leshy','uncommon','blood',2, 2, 4, 'strafe', '',1,1,0),
('pronghorn', 'Pronghorn', 'hooved','leshy','uncommon','blood',2, 1, 3, 'strafe,splitstrike', '',1,1,0),
('moose', 'Moose Buck', 'hooved','leshy','uncommon','blood',3, 3, 7, 'strafepush', '',1,1,0),
-- insect
('mantisgod', 'Mantis God', 'insect','leshy','rare','blood',1, 1, 1, 'tristrike', '',1,1,0),
('mothman_1', 'Strange Larva', 'insect','leshy','rare','blood',1, 0, 3, 'evolve:mothman_2', '',1,1,0),
('bee', 'Bee', 'insect','leshy','common','none',0, 1, 1, 'flying', '',1,1,0),
('beehive', 'Beehive', 'insect','leshy','common','blood',1, 0, 2, 'beesonhit', '',1,1,0),
('mantis', 'Mantis', 'insect','leshy','common','blood',1, 1, 1, 'splitstrike', '',1,1,0),
('ringworm', 'Ringworm', 'insect','leshy','common','blood',1, 0, 1, '', '',1,1,0),
('ant', 'Ant', 'insect','leshy','common','blood',1, 0, 2, '', 'Ant',1,1,0),
('antqueen', 'Ant Queen', 'insect','leshy','uncommon','blood',2, 0, 3, 'drawant', 'Ant',1,1,0),
('stinkbug', 'Stinkbug', 'insect','leshy','common','bones',2, 1, 2, 'debuffenemy', '',1,1,0),
('cockroach', 'Cockroach', 'insect','leshy','uncommon','bones',4, 1, 1, 'drawcopyondeath', '',1,1,0),
('maggots', 'Corpse Maggots', 'insect','leshy','uncommon','bones',5, 1, 2, 'corpseeater', '',1,1,0),
-- reptile
('geck', 'Geck', 'reptile','leshy','rare','none',0, 1, 1, '', '',1,1,0),
('ouroboros', 'Ouroboros', 'reptile','leshy','rare','blood',2, 1, 1, 'drawcopyondeath', 'ouroboros',1,1,0),
('bullfrog', 'Bullfrog', 'reptile','leshy','common','blood',1, 1, 2, 'reach', '',1,1,0),
('skink', 'Skink', 'reptile','leshy','common','blood',1, 1, 2, 'tailonhit', '',1,1,0),
('adder', 'Adder', 'reptile','leshy','uncommon','blood',2, 1, 1, 'deathtouch', '',1,1,0),
('turtle', 'River Snapper', 'reptile','leshy','uncommon','blood',2, 1, 6, '', '',1,1,0),
('rattler', 'Rattler', 'reptile','leshy','uncommon','bones',6, 3, 1, '', '',1,1,0),
-- pelts
('pelt_hare', 'Rabbit Pelt', 'pelt','leshy','ground','none',0, 0, 1, '', '',1,1,0),
('pelt_wolf', 'Wolf Pelt', 'pelt','leshy','ground','none',0, 0, 2, '', '',1,1,0),
('pelt_golden', 'Golden Pelt', 'pelt','leshy','ground','none',0, 0, 3, '', '',1,1,0),
-- miscellaneous
('moleman', 'Mole Man', 'none','leshy','rare','blood',1, 0, 6, 'reach,whackamole', '',1,1,0),
('amalgam', 'Amalgam', 'bird,canine,hooved,insect,reptile,squirrel','leshy','rare','blood',2, 3, 3, '', '',1,1,0),
('packrat', 'Pack Rat', 'none','leshy','rare','blood',2, 2, 2, 'randomconsumable', '',1,1,0),
('daus', 'The Daus', 'none','leshy','rare','blood',2, 2, 2, 'createbells', 'chimekeeper',1,1,0),
('urayuli', 'Urayuli', 'none','leshy','rare','blood',4, 7, 7, '', '',1,1,0),
('amoeba', 'Amoeba', 'none','leshy','rare','bones',2, 1, 2, 'randomability', '',1,1,0),
('cat', 'Cat', 'none','leshy','common','blood',1, 0, 1, 'sacrificial', 'cat',1,1,0),
('squidcards', '~~~~~~~~~~~~~~', 'none','leshy','common','blood',1, 0, 1, '', 'cardcounter',1,1,0),
('squidmirror', '~~~~~~~~~~~~~~', 'none','leshy','common','blood',1, 0, 3, '', 'mirrorcounter',1,1,0),
('mole', 'Mole', 'none','leshy','common','blood',1, 0, 4, 'whackamole', '',1,1,0),
('porcupine', 'Porcupine', 'none','leshy','common','blood',1, 1, 2, 'sharp', '',1,1,0),
('otter', 'Otter', 'none','leshy','common','blood',1, 1, 4, 'submerge', '',1,1,0),
('skunk', 'Skunk', 'none','leshy','common','blood',1, 0, 3, 'debuffenemy', '',1,1,0),
('stoat', 'Stoat', 'none','leshy','common','blood',1, 1, 3, '', '',1,1,0),
('warren', 'Warren', 'none','leshy','common','blood',1, 0, 2, 'drawrabbits', '',1,1,0),
('beaver', 'Beaver', 'none','leshy','uncommon','blood',2, 1, 3, 'createdams', '',1,1,0),
('squidbell', '~~~~~~~~~~~~~~', 'none','leshy','uncommon','blood',2, 0, 3, '', 'bellcounter',1,1,0),
('fieldmice', 'Field Mice', 'none','leshy','uncommon','blood',2, 2, 2, 'drawcopy', '',1,1,0),
('ratking', 'Rat King', 'none','leshy','uncommon','blood',2, 2, 1, 'quadruplebones', '',1,1,0),
('shark', 'Great White', 'none','leshy','uncommon','blood',3, 4, 2, 'submerge', '',1,1,0),
('grizzly', 'Grizzly', 'none','leshy','uncommon','blood',3, 4, 6, '', '',1,1,0),
('opossum', 'Opossum', 'none','leshy','uncommon','bones',2, 1, 1, '', '',1,1,0),
('bat', 'Bat', 'none','leshy','uncommon','bones',4, 2, 1, 'flying', '',1,1,0),
-- non-obtainable
('boulder', 'Boulder', 'none','leshy','ground','none',0, 0, 5, '', '',0,1,0),
('dausbell', 'Bell', 'none','leshy','ground','none',0, 0, 1, '', '',0,1,0),
('vertebrae', 'Vertebrae', 'none','leshy','common','none',0, 0, 1, '', '',0,1,0),
('dam', 'dam', 'none','leshy','ground','none',0, 0, 2, '', '',0,1,0),
('frozen_opossum', 'Frozen Opossum', 'none','leshy','ground','none',0, 0, 5, 'icecube', '',0,1,0),
('rabbit', 'Rabbit', 'none','leshy','common','none',0, 0, 1, '', '',0,1,0),
('smoke', 'The Smoke', 'none','leshy','common','none',0, 0, 1, 'quadruplebones', '',0,1,0),
('smoke_improved', 'Greater Smoke', 'none','leshy','common','none',0, 1, 3, 'quadruplebones', '',0,1,0),
('mothman_2', 'Strange Pupa', 'insect','leshy','rare','blood',1, 0, 3, 'evolve:mothman', '',0,1,0),
('mothman_3', 'Mothman', 'insect','leshy','rare','blood',1, 7, 3, 'flying', '',0,1,0),
('cat_undead', 'Undead Cat', 'none','leshy','common','blood',1, 3, 6, '', '',0,1,0),
('baitbucket', 'Bait Bucket', 'none','leshy','ground','none',0, 0, 1, '', 'baitbucket',0,1,0),
('goldnugget', 'Gold Nugget', 'none','leshy','ground','none',0, 0, 2, '', '',0,1,0),
('tree', 'Grand Fir', 'none','leshy','ground','none',0, 0, 2, 'reach', '',0,1,0),
('trap', 'Trap', 'none','leshy','ground','none',0, 0, 1, 'reach,steeltrap', 'trap_closed',0,1,0),
('packmule', 'Pack Mule', 'hooved','leshy','common','none',0, 0, 5, 'strafe', '',0,1,0),
('tree_snowcovered', 'Snowy Fir', 'none','leshy','ground','none',0, 0, 4, 'reach', '',0,1,0),
('starvingman', 'Starvation', 'none','leshy','common','none',0, 1, 1, 'preventattack', '',0,1,0),
('trapfrog', 'Strange Frog', 'none','leshy','ground','blood',1, 1, 2, 'reach', 'trapspawner',0,1,0),
('stump', 'Stump', 'none','leshy','ground','none',0, 0, 3, '', '',0,0,0),
-- kaycee
('cuckoo', 'Cuckoo', 'bird','leshy','common','blood',1, 1, 1, 'createegg,flying', '',1,1,0),
('lammergeier', 'Lammergeier', 'bird','leshy','uncommon','blood',3, 0, 4, 'flying', 'Lammergeier',1,1,0),
('direwolfcub', 'Dire Wolf cub', 'canine','leshy','uncommon','blood',2, 1, 1, 'evolve:direwolf,bonedigger', '',1,1,0),
('direwolf', 'Dire Wolf', 'canine','leshy','uncommon','blood',3, 2, 5, 'doublestrike', '',1,1,0),
('redhart', 'Red Hart', 'hooved','leshy','uncommon','blood',2, 0, 2, 'strafe', 'redhart',1,1,0),
('bull', 'Wild Bull', 'hooved','leshy','uncommon','blood',2, 3, 2, 'strafeswap', '',1,1,0),
('lice', 'Pelt Lice', 'insect','leshy','rare','blood',4, 1, 1, 'doublestrike', 'petlice',1,1,0),
('antflying', 'Flying Ant', 'insect','leshy','common','blood',1, 0, 1, 'flying', 'ant',1,1,0),
('mealworm', 'Mealworm', 'insect','leshy','common','bones',2, 0, 2, 'morsel', '',1,1,0),
('tadpole', 'Tadpole', 'reptile','leshy','common','none',0, 0, 1, 'evolve:frog,submerge', '',1,1,0),
('mudturtle', 'Mud Turtle', 'reptile','leshy','uncommon','blood',2, 2, 2, 'deathshield', '',1,1,0),
('kraken', 'Kraken', 'none','leshy','rare','blood',1, 1, 1, 'submergesquid', '',1,1,0),
('hodag', 'Hodag', 'none','leshy','rare','blood',2, 1, 5, 'gainattackonkill', '',1,1,0),
('aquasquirrel', 'Aqua Squirrel', 'squirrel','neutral','common','none',0, 0, 1, 'submerge', '',0,1,0),
('raccoon', 'Raccoon', 'none','leshy','common','blood',1, 1, 1, 'opponentbones', '',1,1,0),
('wolverine', 'Wolverine', 'none','leshy','uncommon','bones',5, 1, 3, 'gainattackonkill', '',1,1,0),
('brokenegg', 'Broken Egg', 'none','leshy','ground','none',0, 0, 1, '', '',0,1,0),
('moleseaman', 'Mole Seaman', 'none','leshy','rare','blood',1, 1, 8, 'whackamole,reach', '',1,1,0),
('skeletonpirate', 'Skeleton Pirate', 'none','leshy','common','none',0, 2, 1, 'brittle', '',1,1,0),
-- grimora (bones)
('skeleton', 'Skeleton', 'none','grimora','common','bones',1, 1, 1, '', '',1,0,0),
('skeletonparrot', 'Skeleton Parrot', 'none','grimora','common','bones',1, 1, 1, 'flying', '',1,0,0),
('bonehound', 'Bonehound', 'none','grimora','common','bones',2, 2, 3, '', '',1,0,0),
('gravedigger', 'Gravedigger', 'none','grimora','common','bones',2, 1, 3, 'bonedigger', '',1,0,0),
('banshee', 'Banshee', 'none','grimora','uncommon','bones',2, 2, 1, 'flying', '',1,0,0),
('revenant', 'Revenant', 'none','grimora','uncommon','bones',3, 3, 3, '', '',1,0,0),
('skeletonpirate', 'Skeleton Pirate', 'none','grimora','uncommon','bones',4, 3, 4, 'deathshield', '',1,0,0),
('franknstein', 'Franknstein', 'none','grimora','rare','bones',5, 5, 5, 'drawcopyondeath', '',1,0,0),
-- p03 (energy)
('automaton', 'Automaton', 'none','p03','common','energy',1, 2, 1, '', '',1,0,0),
('bolthound', 'Bolthound', 'none','p03','common','energy',2, 2, 3, '', '',1,0,0),
('leapbot', 'Leap Bot', 'none','p03','common','energy',2, 1, 3, 'strafe', '',1,0,0),
('sentrybot', 'Sentry Bot', 'none','p03','uncommon','energy',1, 1, 2, 'sentry', '',1,0,0),
('minecart', 'Mine Cart', 'none','p03','uncommon','energy',1, 1, 1, 'explodeondeath', '',1,0,0),
('batterybot', 'Battery Bot', 'none','p03','uncommon','energy',2, 0, 2, 'gainbattery', '',1,0,0),
('shieldbot', 'Shield Bot', 'none','p03','uncommon','energy',2, 1, 3, 'deathshield', '',1,0,0),
('gunnerbot', 'Gunner Bot', 'none','p03','uncommon','energy',3, 3, 2, 'splitstrike', '',1,0,0),
('ourobot', 'Ourobot', 'none','p03','rare','energy',4, 1, 1, 'drawcopyondeath', '',1,0,0),
-- magnificus (gems)
('sapphiremox', 'Sapphire Mox', 'none','magnificus','common','none',0, 0, 1, 'gaingemblue', '',1,0,0),
('rubymox', 'Ruby Mox', 'none','magnificus','common','none',0, 0, 1, 'gaingemorange', '',1,0,0),
('emeraldmox', 'Emerald Mox', 'none','magnificus','common','none',0, 0, 1, 'gaingemgreen', '',1,0,0),
('bluemage', 'Blue Mage', 'none','magnificus','common','bluegem',1, 1, 3, '', '',1,0,0),
('orangemage', 'Orange Mage', 'none','magnificus','common','orangegem',1, 1, 3, '', '',1,0,0),
('practicemage', 'Practice Mage', 'none','magnificus','common','greengem',1, 1, 2, '', '',1,0,0),
('gemshielder', 'Gem Shielder', 'none','magnificus','uncommon','bluegem',1, 1, 2, 'shieldgems', '',1,0,0),
('gemexploder', 'Gem Exploder', 'none','magnificus','uncommon','greengem',2, 1, 1, 'explodegems', '',1,0,0),
('rubygolem', 'Ruby Golem', 'none','magnificus','uncommon','orangegem',3, 3, 3, '', '',1,0,0),
('gemfiend', 'Gem Fiend', 'none','magnificus','rare','bluegem',4, 4, 4, 'buffgems', '',1,0,0),
-- kaycee (suite)
('zombieparrot', 'Zombie Parrot', 'none','leshy','common','none',0, 2, 3, 'brittle,flying', '',1,0,0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), tribe = VALUES(tribe), scribe = VALUES(scribe), rarity = VALUES(rarity),
    cost_type = VALUES(cost_type), cost_amount = VALUES(cost_amount),
    attack = VALUES(attack), health = VALUES(health), sigils = VALUES(sigils),
    special_ability = VALUES(special_ability), is_obtenable = VALUES(is_obtenable),
    repainted = VALUES(repainted), is_repainted = VALUES(is_repainted);

-- ===== BOUTIQUE =====
-- INSCRYPCARDS - Boutique (packs). Les cartes sont dans cards.sql.
-- image = texture de assets/images (sans .png) ; kind scribe = card_count cartes aléatoires du Scribe,
-- repaint = 1 variante de portrait, face = 1 top + 1 face + 1 bottom d'avatar.

SET NAMES utf8mb4;

INSERT INTO shop_packs (slug, name, description, image, price, card_count, kind, scribe, sort_order) VALUES
('beast',      'Beast Pack',       '5 random Leshy cards',        'Beast_pack',      5, 5, 'scribe',  'leshy',      1),
('undead',     'Undead Pack',      '5 random Grimora cards',      'Undead_pack',     5, 5, 'scribe',  'grimora',    2),
('technology', 'Technology Pack',  '5 random P03 cards',          'Technology_pack', 5, 5, 'scribe',  'p03',        3),
('magic',      'Magic Pack',       '5 random Magnificus cards',   'Magic_pack',      5, 5, 'scribe',  'magnificus', 4),
('repaint',    'Repaint Pack',     '1 random repainted portrait', 'Repaint_pack',    5, 1, 'repaint', NULL,         5),
('face',       'Face Pack',        '1 top, 1 face and 1 bottom',  'Face_pack',       5, 3, 'face',    NULL,         6)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), description = VALUES(description), image = VALUES(image),
    price = VALUES(price), card_count = VALUES(card_count), kind = VALUES(kind),
    scribe = VALUES(scribe), sort_order = VALUES(sort_order);
