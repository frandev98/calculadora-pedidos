package com.francisco.calculadorapedidos.data
import com.francisco.calculadorapedidos.R

object ProductCatalog {
    val masterList = listOf(
        // --- LÍNEA LIMPIA ---
        Product(1, "145879", "Alpha Balance", "LÍNEA LIMPIA", 20.0, 129.50, "Dp 28 x 5g",false, R.drawable.alpha_balance),
        Product(2, "146065", "Berry Balance", "LÍNEA LIMPIA", 26.0, 169.00, "Dp 28 x 5g", false, R.drawable.berry_balance),
        Product(3, "142953", "Flora Liv", "LÍNEA LIMPIA", 24.0, 154.00, "Dp 28 x 5g", false, R.drawable.flora_liv),
        Product(4, "144289", "Liquid Fiber", "LÍNEA LIMPIA", 16.0, 105.00, "Caja 28 x 5g", false, R.drawable.liquid_fiber),
        Product(5, "142626", "Prunex1", "LÍNEA LIMPIA", 10.0, 76.00, "Dp 28 x 5g", false, R.drawable.prunex1),
        Product(6, "144137", "Prunex1 (Pequeño)", "LÍNEA LIMPIA", 2.0, 21.00, "Caja 7 x 5g", false, R.drawable.prunex1_small),
        Product(7, "145993", "Rexet", "LÍNEA LIMPIA", 20.0, 129.50, "Dp 28 x 5g", false, R.drawable.rexet),
        Product(8, "144175", "Rexet (Pequeño)", "LÍNEA LIMPIA", 5.0, 36.50, "Caja 7 x 5g", false, R.drawable.rexet_small),
        Product(9, "144404", "Pack Detox", "LÍNEA LIMPIA", 24.0, 175.00, "Caja Pack", false, R.drawable.pack_detox),

        // --- LÍNEA ANTI-EDAD ---
        Product(10, "143065", "Beauty-In", "LÍNEA ANTI-EDAD", 25.0, 163.00, "Dp 28 x 5g", false, R.drawable.beauty_in),
        Product(11, "145082", "Youth Elixir", "LÍNEA ANTI-EDAD", 20.0, 129.50, "Dp 28 x 5g", false, R.drawable.youth_elixir),
        Product(12, "143142", "Golden Flx", "LÍNEA ANTI-EDAD", 22.0, 143.00, "Dp 28 x 5g", false, R.drawable.golden_flx),
        Product(13, "142477", "Passion", "LÍNEA ANTI-EDAD", 20.0, 129.50, "Caja 28 x 5g", false, R.drawable.passion),
        Product(14, "142472", "Probal", "LÍNEA ANTI-EDAD", 25.0, 162.50, "Caja 28 x 5g", false, R.drawable.probal),

        // --- NUTRICIÓN Y REGENERACIÓN ---
        Product(15, "143205", "Protein Active Vainilla/Canela", "NUTRICIÓN Y REGENERACIÓN", 20.0, 141.50, "Caja 14 x 25g", false, R.drawable.protein_active_vainilla),
        Product(16, "143206", "Protein Active Choco/Avellanas", "NUTRICIÓN Y REGENERACIÓN", 20.0, 141.50, "Caja 14 x 25g", false, R.drawable.protein_active_choco),
        Product(17, "142169", "Biopro+ Tect", "NUTRICIÓN Y REGENERACIÓN", 18.0, 119.50, "Caja 14 x 25g", false, R.drawable.biopro_tect),
        Product(18, "141503", "Biopro+ Tect (Pote)", "NUTRICIÓN Y REGENERACIÓN", 23.0, 163.00, "Pote 500g", false, R.drawable.biopro_tect_pote),

        // --- ENERGÍA Y REVITALIZACIÓN ---
        Product(19, "10105", "Vitaenergía", "ENERGÍA Y REVITALIZACIÓN", 20.0, 129.50, "Caja 30 x 7.5g", false, R.drawable.vitaenergia),
        Product(20, "145716", "Nutraday", "ENERGÍA Y REVITALIZACIÓN", 20.0, 129.50, "Dp 28 x 5g", false, R.drawable.nutraday),
        Product(21, "142625", "Vita Xtra T+", "ENERGÍA Y REVITALIZACIÓN", 20.0, 129.50, "Dp 28 x 5g", false, R.drawable.vita_xtra),
        Product(22, "144133", "Vita Xtra T+ (Pequeño)", "ENERGÍA Y REVITALIZACIÓN", 5.0, 36.50, "Caja 7 x 5g", false, R.drawable.vita_xtra_small),
        Product(23, "146291", "Xpeed Guaraná", "ENERGÍA Y REVITALIZACIÓN", 2.0, 39.50, "Pack x 4", false, R.drawable.xpeed),

        // --- IMUNOLÓGICA ---
        Product(24, "143069", "Vera+", "IMUNOLÓGICA", 26.0, 169.00, "Dp 28 x 5g", false, R.drawable.vera_plus),
        Product(25, "142627", "Gano+ Cappuccino", "IMUNOLÓGICA", 12.0, 92.50, "Dp 28 x 7.5g", false, R.drawable.gano_cappuccino),
        Product(26, "146264", "Gano+ T", "IMUNOLÓGICA", 12.0, 92.50, "Dp 28 x 5g", false, R.drawable.gano_t),
        Product(27, "141293", "Café Ganomax", "IMUNOLÓGICA", 22.0, 146.00, "Caja 28 x 5g", false, R.drawable.cafe_ganomax),

        // --- CONTROL DE PESO Y MEDIDAS ---
        Product(28, "142623", "Thermo T3", "CONTROL DE PESO Y MEDIDAS", 20.0, 129.50, "Dp 28 x 5g", false, R.drawable.thermo_t3),
        Product(29, "144136", "Thermo T3 (Pequeño)", "CONTROL DE PESO Y MEDIDAS", 5.0, 36.50, "Caja 7 x 5g", false, R.drawable.thermo_t3_small),
        Product(30, "142624", "Nocarb-T", "CONTROL DE PESO Y MEDIDAS", 20.0, 129.50, "Dp 28 x 5g", false, R.drawable.nocarb_t),
        Product(31, "143207", "Protein Active Fit Vainilla", "CONTROL DE PESO Y MEDIDAS", 21.0, 149.00, "Caja 14 x 25g", false, R.drawable.protein_fit_vainilla),
        Product(32, "143208", "Protein Active Fit Choco", "CONTROL DE PESO Y MEDIDAS", 21.0, 149.50, "Caja 14 x 25g", false, R.drawable.protein_fit_choco),
        Product(33, "142167", "Biopro+ Fit", "CONTROL DE PESO Y MEDIDAS", 16.0, 108.00, "Caja 14 x 25g", false, R.drawable.biopro_fit),
        Product(34, "142456", "Protein Xoup Crema Criolla", "CONTROL DE PESO Y MEDIDAS", 10.0, 68.00, "Caja 7 x 25g", false, R.drawable.protein_xoup_criolla),
        Product(35, "142455", "Protein Xoup Brócoli", "CONTROL DE PESO Y MEDIDAS", 10.0, 68.00, "Caja 7 x 25g", false, R.drawable.protein_xoup_brocoli),
        Product(36, "142457", "Protein Xoup Espárragos", "CONTROL DE PESO Y MEDIDAS", 10.0, 68.00, "Caja 7 x 25g", false, R.drawable.protein_xoup_esparrago),
        Product(37, "141446", "Café & Café Fit", "CONTROL DE PESO Y MEDIDAS", 24.0, 159.50, "Caja 28 x 5g", false, R.drawable.cafe_fit),
        Product(38, "141449", "Café & Café Fit Cappuccino", "CONTROL DE PESO Y MEDIDAS", 16.0, 109.50, "Dp 14 x 15g", false, R.drawable.cafe_fit_capuccino),
        Product(39, "142478", "Chocolate Fit", "CONTROL DE PESO Y MEDIDAS", 12.0, 92.50, "Caja 14 x 15g", false, R.drawable.chocolate_fit),
        Product(40, "144525", "Chocolate Fit Navideño", "CONTROL DE PESO Y MEDIDAS", 12.0, 92.50, "Caja 14 x 15g", false, R.drawable.chocolate_fit),
        Product(41, "145259", "Pack 5/14", "CONTROL DE PESO Y MEDIDAS", 60.0, 399.00, "Caja Pack", false, R.drawable.pack_514),
        Product(42, "144915", "Pack 5/14 Active", "CONTROL DE PESO Y MEDIDAS", 64.0, 435.00, "Caja Pack", false, R.drawable.pack_514_active),

        // --- VIGOR MENTAL ---
        Product(43, "143070", "No Stress", "VIGOR MENTAL", 22.0, 142.50, "Dp 28 x 5g", false, R.drawable.no_stress),
        Product(44, "144135", "No Stress (Pequeño)", "VIGOR MENTAL", 5.0, 36.50, "Caja 7 x 5g", false, R.drawable.no_stress_small),
        Product(45, "143066", "On", "VIGOR MENTAL", 16.0, 105.00, "Dp 28 x 5g", false, R.drawable.on_fuxion),
        Product(46, "144134", "On (Pequeño)", "VIGOR MENTAL", 4.0, 29.00, "Caja 7 x 5g", false, R.drawable.on_fuxion_small),

        // --- SPORT ---
        Product(47, "143286", "Protein Active Sport Vainilla", "SPORT", 22.0, 156.00, "Caja 14 x 25g", false, R.drawable.protein_active_sport_vainilla),
        Product(48, "143287", "Protein Active Sport Choco", "SPORT", 22.0, 156.50, "Caja 14 x 25g", false, R.drawable.protein_active_sport_choco),
        Product(49, "143288", "Biopro+ Sport", "SPORT", 20.0, 132.50, "Caja 14 x 25g", false, R.drawable.biopro_sport_caja),
        Product(50, "141502", "Biopro+ Sport (Pote)", "SPORT", 36.0, 259.50, "Pote 2lb", false, R.drawable.biopro_sport_pote),
        Product(51, "143284", "Pre Sport", "SPORT", 22.0, 143.00, "Caja 28 x 5g", false, R.drawable.pre_sport),
        Product(52, "144192", "Pre Sport (Pequeño)", "SPORT", 5.0, 38.50, "Caja 7 x 5g", false, R.drawable.pre_sport),
        Product(53, "143283", "Xtra Mile", "SPORT", 20.0, 129.50, "Caja 28 x 5g", false, R.drawable.xtra_mile),
        Product(54, "144191", "Xtra Mile (Pequeño)", "SPORT", 5.0, 36.50, "Caja 7 x 5g", false, R.drawable.vita_xtra_small),
        Product(55, "143285", "Post Sport", "SPORT", 22.0, 143.00, "Caja 28 x 5g", false, R.drawable.post_sport),
        Product(56, "144193", "Post Sport (Pequeño)", "SPORT", 5.0, 38.50, "Caja 7 x 5g", false, R.drawable.post_sport),

        // --- LÍNEA GASTRONÓMICA ---
        Product(57, "147146", "Q'Ocina Base Verde", "LÍNEA GASTRONÓMICA", 2.0, 24.00, "Sobre x 50g", false, R.drawable.no_img),
        Product(58, "147147", "Q'Ocina Base Amarilla", "LÍNEA GASTRONÓMICA", 2.0, 24.00, "Sobre x 50g", false, R.drawable.no_img),
        Product(59, "147148", "Q'Ocina Base Roja", "LÍNEA GASTRONÓMICA", 2.0, 24.00, "Sobre x 50g", false, R.drawable.no_img),
        Product(60, "147232", "PROBIX", "LÍNEA GASTRONÓMICA", 20.0, 129.50, "Caja 28 x 0.5g", false, R.drawable.no_img),

        // --- COSMECÉUTICA ---
        Product(61, "144377", "LUSSÔME Sérum Ojos", "COSMECÉUTICA", 20.0, 174.50, "15 ml", false, R.drawable.no_img),
        Product(62, "144378", "LUSSÔME Sérum Hidratante", "COSMECÉUTICA", 22.0, 192.00, "50 ml", false, R.drawable.no_img),
        Product(63, "144379", "LUSSÔME Sérum Regenerador", "COSMECÉUTICA", 24.0, 208.00, "50 ml", false, R.drawable.no_img),
        Product(64, "144380", "GALEON XXI Shampoo", "COSMECÉUTICA", 6.0, 50.50, "250 ml", false, R.drawable.no_img),
        Product(65, "144382", "GALEON XXI After Shave", "COSMECÉUTICA", 5.0, 42.50, "150 ml", false, R.drawable.no_img),
        Product(66, "144381", "GALEON XXI Shampoo Barba", "COSMECÉUTICA", 5.0, 42.50, "150 ml", false, R.drawable.no_img)
    )
}