-- ============================================================================
-- Mob definisi (monster_template) untuk boss event Persimpangan Kematian
-- (Event Kemerdekaan / PersimpanganKematian.java, DEFAULT_WAVES).
--
-- PENTING:
--   - Kolom hp & dame di sini TIDAK dipakai saat boss di-spawn — di
--     startWave() nilainya SELALU di-override pakai mob_hp/mob_damage (dan
--     mob_def, kalau kamu isi tabel event_wave_config) dari DEFAULT_WAVES
--     atau tabel event_wave_config. Jadi angka hp/dame di bawah cuma
--     placeholder biar baris valid, aman diubah sesuka hati tanpa
--     mempengaruhi event. (Defense/mob_def diatur lewat kolom `mob_def` di
--     event_wave_config, BUKAN dari monster_template ini.)
--   - typeMove = 10 WAJIB (dipakai kode buat nandain ini "AI world boss").
--   - id 221/223/226/229/234 SENGAJA dipilih beda dari id boss_premium
--     (233 Rock Joe, 235 Dagnu, 236 Dagon, 238 Blink) supaya tidak
--     numpuk/duplikat sama world boss map 111-114. JANGAN pakai ulang id
--     yang sudah dipakai monster/boss lain di tabel monster_template kamu —
--     cek dulu sebelum jalankan (lihat query cek di paling bawah).
--   - Kalau tabel monster_template kamu punya kolom lain yang NOT NULL tanpa
--     default (mis. image, exp, def, dll — kode ini cuma baca kolom
--     id/name/level/hp/dame/typeMove lewat "SELECT *"), tambahkan kolom itu
--     ke INSERT di bawah sesuai skema kamu, kalau tidak INSERT bisa gagal.
--
-- Cara pakai: jalankan sekali di database server. Aman dijalankan ulang
-- (ON DUPLICATE KEY UPDATE) kalau perlu update data belakangan.
-- ============================================================================

INSERT INTO `monster_template` (`id`, `name`, `level`, `hp`, `dame`, `typeMove`)
VALUES
    (221, 'Malaikat Maut',  150,  3000000,  30000, 10),
    (223, 'Kunti Mantan',   165,  5000000,  50000, 10),
    (226, 'Goblin Etanol',  180,  8000000,  80000, 10),
    (229, 'Kecoa',          195, 12000000, 120000, 10),
    (234, 'Dagan',          210, 20000000, 200000, 10)
ON DUPLICATE KEY UPDATE
    `name`     = VALUES(`name`),
    `level`    = VALUES(`level`),
    `hp`       = VALUES(`hp`),
    `dame`     = VALUES(`dame`),
    `typeMove` = VALUES(`typeMove`);

-- Cek dulu sebelum insert kalau ragu id-nya sudah dipakai monster lain:
-- SELECT * FROM `monster_template` WHERE `id` IN (221, 223, 226, 229, 234);

-- Setelah baris ini ada, restart server (MobTemplate.entrys dimuat sekali
-- saat startup dari core/Manager.java) ATAU pastikan ada mekanisme reload
-- template kalau server kamu punya command semacam itu.
