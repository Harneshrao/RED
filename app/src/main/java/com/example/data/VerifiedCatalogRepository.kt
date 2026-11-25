package com.example.data

import android.net.Uri
import com.example.model.CatalogComponentTemplate
import com.example.model.CompatibilityStatus
import com.example.model.ConfidenceLevel
import com.example.model.MarketplaceSampleListing
import com.example.model.MarketplaceSearchChannel
import com.example.model.ProductCategory
import com.example.model.RepairGuideInfo
import com.example.model.ReplacementDifficulty
import com.example.model.ScanComponentDoc
import com.example.model.ScanDoc
import com.example.model.ScanResultBundle
import com.example.model.VerificationProvenance

object VerifiedCatalogRepository {

    val watchComponentTemplates: List<CatalogComponentTemplate> = listOf(
        CatalogComponentTemplate(
            key = "watch_crystal",
            name = "Watch Crystal",
            alternativeNames = listOf("Sapphire glass", "Mineral crystal", "Watch lens"),
            primaryFunction = "Protects the dial and hands from dust, moisture, and impact while maintaining optical clarity.",
            defaultMaterial = "Sapphire or Mineral Glass (verify material)",
            measurementGuidance = "Measure outer diameter to 0.1 mm and edge thickness with digital calipers after crystal removal.",
            standardSizes = listOf("28.0 mm", "30.5 mm", "31.5 mm", "32.0 mm"),
            defaultDifficulty = ReplacementDifficulty.ADVANCED,
            layerOrder = 0,
            repairGuide = RepairGuideInfo(
                componentKey = "watch_crystal",
                componentName = "Watch Crystal",
                problemIdentification = "Scratches, edge chipping, spider fractures, or compromised water resistance.",
                requiredTools = listOf("Case back opener", "Movement holder", "Crystal press with nylon dies", "Digital calipers (0.01 mm)", "Microfiber blower"),
                steps = listOf(
                    "Open the case back using an appropriate case wrench and remove the crown stem by pressing the movement detent.",
                    "Lift the movement and dial assembly cleanly into a dust-free tray.",
                    "Press the damaged crystal outward from the inside using a crystal press with a properly sized flat nylon die.",
                    "Inspect and replace the crystal compression gasket (I-ring or L-gasket) if flattened or cracked.",
                    "Measure the exact inner bezel/rehaut lip diameter with calipers before ordering a replacement crystal.",
                    "Align the new crystal evenly and press gently until flush, then pressure-test water resistance."
                ),
                difficulty = ReplacementDifficulty.ADVANCED,
                estimatedMinutesRange = "30–45 min",
                safetyWarnings = listOf(
                    "Wear eye protection when pressing fractured glass crystals.",
                    "Never press against the watch dial or hands."
                ),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "bezel",
            name = "Bezel & Insert",
            alternativeNames = listOf("Rotating bezel", "Chapter ring bezel", "Dive bezel"),
            primaryFunction = "Retains the crystal perimeter or provides a rotatable elapsed-time scale.",
            defaultMaterial = "316L Stainless Steel / Ceramic or Aluminum Insert",
            measurementGuidance = "Measure bezel outer diameter, inner crystal opening, and insert inner/outer diameters.",
            standardSizes = listOf("38.0 mm x 31.5 mm", "40.0 mm x 30.6 mm"),
            defaultDifficulty = ReplacementDifficulty.MODERATE,
            layerOrder = 1,
            repairGuide = RepairGuideInfo(
                componentKey = "bezel",
                componentName = "Bezel & Insert",
                problemIdentification = "Stiff/seized rotation, missing click spring, or scratched/faded bezel insert.",
                requiredTools = listOf("Case knife / bezel pry tool", "Protective tape", "Isopropyl alcohol", "3M bezel adhesive ring"),
                steps = listOf(
                    "Apply protective masking tape around the watch lugs to prevent case scratches.",
                    "Insert a thin nylon-tipped bezel knife at the 10 o'clock recess and gently pop the bezel assembly upward.",
                    "Clean dried grime from the bezel groove and verify the click spring wire is seated flat.",
                    "Warm or dissolve old adhesive to remove a damaged insert, clean the channel with isopropyl alcohol, and bond the new insert aligned to 12 o'clock."
                ),
                difficulty = ReplacementDifficulty.MODERATE,
                estimatedMinutesRange = "15–25 min",
                safetyWarnings = listOf("Do not pry near the crown guards; excessive force can bend the bezel ring."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "dial",
            name = "Dial",
            alternativeNames = listOf("Watch face", "Index plate"),
            primaryFunction = "Displays hour markers, minute track, and apertures for date or sub-registers.",
            defaultMaterial = "Brass base with lacquer or sunburst plating",
            measurementGuidance = "Verify dial diameter (commonly 28.5 mm for Seiko-pattern movements), dial foot positions, and date window alignment (3 o'clock vs 3.8 o'clock).",
            standardSizes = listOf("28.5 mm (Caliber-specific feet)", "29.0 mm", "31.0 mm"),
            defaultDifficulty = ReplacementDifficulty.PROFESSIONAL,
            layerOrder = 2,
            repairGuide = RepairGuideInfo(
                componentKey = "dial",
                componentName = "Dial",
                problemIdentification = "Discoloration, moisture spotting, detached applied indices, or broken dial feet.",
                requiredTools = listOf("Hand levers & dial protector", "Rodico cleaning putty", "Brass tweezers", "Dust blower"),
                steps = listOf(
                    "Remove movement from case and slip a thin plastic dial protector beneath the hands.",
                    "Lift hour, minute, and seconds hands vertically using paired hand levers.",
                    "Release dial foot screws or friction clips on the movement spacer ring to lift the dial.",
                    "Confirm replacement dial foot orientation matches your exact movement caliber and crown position."
                ),
                difficulty = ReplacementDifficulty.PROFESSIONAL,
                estimatedMinutesRange = "40–60 min",
                safetyWarnings = listOf("Luminous indices and lacquered dials scratch permanently if touched by bare fingers or steel tools."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "hands",
            name = "Hour, Minute & Seconds Hands",
            alternativeNames = listOf("Watch hands", "Hand set"),
            primaryFunction = "Indicate hours, minutes, and seconds by rotating on concentric movement pinions.",
            defaultMaterial = "Painted brass or rhodium-plated steel with luminous compound",
            measurementGuidance = "Verify exact pinion hole diameters (e.g., 1.50 mm / 0.90 mm / 0.21 mm for NH35/7S26) and clearance lengths.",
            standardSizes = listOf("1.50 / 0.89 / 0.21 mm (7S/NH)", "1.20 / 0.70 / 0.20 mm (Miyota 82)"),
            defaultDifficulty = ReplacementDifficulty.PROFESSIONAL,
            layerOrder = 3,
            repairGuide = RepairGuideInfo(
                componentKey = "hands",
                componentName = "Hour, Minute & Seconds Hands",
                problemIdentification = "Bent hand scraping crystal or dial, loose hand slipping out of time, or degraded lume.",
                requiredTools = listOf("Presto puller or hand levers", "Hand press tips (0.5–1.5 mm)", "Eye loupe (10x)", "Dial protector"),
                steps = listOf(
                    "Advance the time until the date changes cleanly at midnight so hour and minute alignment is indexed.",
                    "Protect the dial and pull the seconds, minute, and hour hands straight upward.",
                    "Inspect pinion tube clearance and press replacement hands down level starting with the hour hand."
                ),
                difficulty = ReplacementDifficulty.PROFESSIONAL,
                estimatedMinutesRange = "30–50 min",
                safetyWarnings = listOf("Seconds hand post (~0.20 mm) snaps easily if pressed off-center."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "crown",
            name = "Crown & Winding Stem",
            alternativeNames = listOf("Winding crown", "Screw-down crown", "Stem"),
            primaryFunction = "Couples external rotation to the movement keyless works for winding and setting time/date.",
            defaultMaterial = "316L Stainless Steel with Viton/Nitrile O-ring",
            measurementGuidance = "Match case tube thread pitch, crown outer diameter, and stem tap size (e.g., Tap 10 / 0.90 mm).",
            standardSizes = listOf("Tap 10 (0.90 mm) • 6.0–7.0 mm head"),
            defaultDifficulty = ReplacementDifficulty.ADVANCED,
            layerOrder = 4,
            repairGuide = RepairGuideInfo(
                componentKey = "crown",
                componentName = "Crown & Winding Stem",
                problemIdentification = "Stripped screw-down threads, bent stem, or failed crown O-ring causing moisture ingress.",
                requiredTools = listOf("Pin vise", "Flush wire cutters", "Fine file", "Loctite 243 threadlocker", "Silicone gasket grease"),
                steps = listOf(
                    "Extract the existing stem via the movement release lever while in time-setting or winding position (per caliber spec).",
                    "Measure exact protrusion from case tube to crown shoulder.",
                    "Trim the new stem incrementally with flush cutters, file flat, and test fit before securing the crown with a micro-drop of threadlocker."
                ),
                difficulty = ReplacementDifficulty.ADVANCED,
                estimatedMinutesRange = "25–40 min",
                safetyWarnings = listOf("Never cut a stem too short on the first trim; measure twice and shave 0.2 mm at a time."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "watch_case",
            name = "Watch Case (Middle Case)",
            alternativeNames = listOf("Case body", "Chassis", "Lug housing"),
            primaryFunction = "Houses and shields the movement, provides structural water resistance, and anchors the strap.",
            defaultMaterial = "316L Stainless Steel / Titanium / Brass",
            measurementGuidance = "Verify lug-to-lug length, lug width (18/20/22 mm), and internal movement spacer diameter.",
            standardSizes = listOf("38–42 mm diameter • 20 mm or 22 mm lugs"),
            defaultDifficulty = ReplacementDifficulty.PROFESSIONAL,
            layerOrder = 5,
            repairGuide = RepairGuideInfo(
                componentKey = "watch_case",
                componentName = "Watch Case",
                problemIdentification = "Deep lug impact, stripped case tube threads, or corroded gasket channels.",
                requiredTools = listOf("Case wrench", "Spring bar tool", "Ultrasonic cleaner (case only)", "Pressure tester"),
                steps = listOf(
                    "Remove bracelet/strap, crown stem, and movement assembly completely before servicing the case.",
                    "Inspect lug holes, crystal seat, and caseback threads for burrs or pitting.",
                    "Replace all rubber gaskets with silicone-lubricated seals upon reassembly."
                ),
                difficulty = ReplacementDifficulty.PROFESSIONAL,
                estimatedMinutesRange = "45–75 min",
                safetyWarnings = listOf("Never place an assembled watch or movement into an ultrasonic cleaner."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "movement",
            name = "Watch Movement (Caliber)",
            alternativeNames = listOf("Caliber", "Engine", "Quartz module"),
            primaryFunction = "Generates and regulates timekeeping oscillations and drives the hands and calendar.",
            defaultMaterial = "Brass plates, steel pinions, synthetic ruby jewels",
            measurementGuidance = "Read the exact caliber stamp engraved on the rotor, mainplate, or caseback before ordering.",
            standardSizes = listOf("Caliber-specific (e.g., 27.4 mm x 5.32 mm for 7S26/NH36)"),
            defaultDifficulty = ReplacementDifficulty.PROFESSIONAL,
            layerOrder = 6,
            repairGuide = RepairGuideInfo(
                componentKey = "movement",
                componentName = "Watch Movement",
                problemIdentification = "Watch stops running, erratic daily rate (> ±45s/day), moisture corrosion, or dead quartz coil/battery.",
                requiredTools = listOf("Case wrench", "Non-magnetic tweezers", "Hand pullers & press", "Timegrapher / demagnetizer"),
                steps = listOf(
                    "For quartz watches, start by testing battery voltage and inspecting contacts for leakage before replacing the movement.",
                    "For mechanical watches, verify whether demagnetizing or a movement swap/service is required.",
                    "Record exact crown position (3 vs 3.8 o'clock) so calendar wheels align properly."
                ),
                difficulty = ReplacementDifficulty.PROFESSIONAL,
                estimatedMinutesRange = "45–90 min",
                safetyWarnings = listOf("Avoid touching the hairspring or balance wheel; magnetic tools will ruin timekeeping."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "case_back",
            name = "Case Back & Gasket",
            alternativeNames = listOf("Caseback", "Rear cover", "O-ring seal"),
            primaryFunction = "Seals the rear of the watch case against water and debris while displaying reference markings.",
            defaultMaterial = "316L Stainless Steel + Nitrile/Viton O-Ring",
            measurementGuidance = "Verify screw-down vs snap-on profile, outer diameter, and gasket cross-section thickness (0.5–0.8 mm).",
            standardSizes = listOf("30–36 mm thread • 0.6 mm O-ring"),
            defaultDifficulty = ReplacementDifficulty.EASY,
            layerOrder = 7,
            repairGuide = RepairGuideInfo(
                componentKey = "case_back",
                componentName = "Case Back & Gasket",
                problemIdentification = "Condensation inside crystal, perished rear O-ring, or damaged caseback notches.",
                requiredTools = listOf("3-prong case wrench or rubber friction ball", "Silicone gasket lubricator", "Brass tweezers"),
                steps = listOf(
                    "Use a rubber friction ball first; if tight, fit a 3-prong case wrench firmly into opposing notches.",
                    "Clean the thread groove with a wooden pegwood stick and inspect the rubber O-ring for cracks or flattening.",
                    "Lubricate a fresh O-ring lightly with silicone grease, seat it flat, and tighten the caseback without cross-threading."
                ),
                difficulty = ReplacementDifficulty.EASY,
                estimatedMinutesRange = "10–15 min",
                safetyWarnings = listOf("Always engage threads backward gently until they click before tightening to avoid cross-threading."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "strap",
            name = "Watch Strap / Bracelet",
            alternativeNames = listOf("Watch band", "Oyster bracelet", "Silicone dive strap"),
            primaryFunction = "Secures the watch head comfortably to the wearer's wrist.",
            defaultMaterial = "316L Stainless Steel, Vulcanized Rubber, or Leather",
            measurementGuidance = "Measure the inside distance between the watch lugs in millimeters (commonly 18, 20, or 22 mm) and note straight vs curved endlinks.",
            standardSizes = listOf("18 mm", "20 mm", "22 mm"),
            defaultDifficulty = ReplacementDifficulty.EASY,
            layerOrder = 8,
            repairGuide = RepairGuideInfo(
                componentKey = "strap",
                componentName = "Watch Strap / Bracelet",
                problemIdentification = "Cracked rubber/leather, stretched bracelet links, or worn endlinks.",
                requiredTools = listOf("Forked spring bar tool", "Digital calipers or mm ruler", "Microfiber cloth"),
                steps = listOf(
                    "Measure the exact lug-to-lug gap with calipers (18 mm, 20 mm, or 22 mm).",
                    "Place the watch face-down on a soft cloth and compress each spring bar flange using the forked tool.",
                    "Align one end of the replacement strap spring bar into the lower lug hole and guide the opposite tip cleanly into the socket until it clicks."
                ),
                difficulty = ReplacementDifficulty.EASY,
                estimatedMinutesRange = "5–10 min",
                safetyWarnings = listOf("Always tug firmly on both sides of the newly fitted strap to verify both spring bar tips are seated inside the lug holes."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "spring_bar",
            name = "Watch Strap Pin (Spring Bar)",
            alternativeNames = listOf("Spring bar", "Lug pin", "Fat spring bar"),
            primaryFunction = "Telescoping spring-loaded stainless pin that anchors the strap or bracelet between the case lugs.",
            defaultMaterial = "316L Stainless Steel",
            measurementGuidance = "Measure lug width (e.g., 20 mm or 22 mm), tube diameter (1.5 mm, 1.8 mm, or 2.5 mm dive), and tip diameter (0.8 mm vs 1.1 mm).",
            standardSizes = listOf("20 mm x 1.8 mm (0.8 mm tip)", "22 mm x 2.5 mm (1.1 mm Seiko dive tip)", "18 mm x 1.5 mm"),
            defaultDifficulty = ReplacementDifficulty.EASY,
            layerOrder = 9,
            repairGuide = RepairGuideInfo(
                componentKey = "spring_bar",
                componentName = "Watch Strap Pin (Spring Bar)",
                problemIdentification = "Bent pin, seized internal spring, or loose strap wobble risking watch drop.",
                requiredTools = listOf("Spring bar tool (forked + pin tip)", "Calipers"),
                steps = listOf(
                    "Compress the flange of the old spring bar from the back of the lug using the forked end of a spring bar tool.",
                    "Verify both lug width (e.g., 20 mm) and whether your watch uses standard 0.8 mm tips or thick 1.1 mm dive-watch tips.",
                    "Seat the new spring bar inside the strap tube, insert one end into the lug hole, compress the opposite flange, and slide into place until it clicks."
                ),
                difficulty = ReplacementDifficulty.EASY,
                estimatedMinutesRange = "5 min",
                safetyWarnings = listOf("Never reuse a bent spring bar; a failed pin can drop the watch."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "clasp",
            name = "Buckle or Clasp",
            alternativeNames = listOf("Deployant clasp", "Folding clasp", "Tang buckle"),
            primaryFunction = "Fastens and micro-adjusts the two halves of the strap or bracelet around the wrist.",
            defaultMaterial = "316L Stainless Steel (milled or stamped)",
            measurementGuidance = "Measure the strap width at the buckle/clasp end (taper width, commonly 16 mm, 18 mm, or 20 mm) rather than the lug width.",
            standardSizes = listOf("16 mm taper", "18 mm taper", "20 mm straight"),
            defaultDifficulty = ReplacementDifficulty.EASY,
            layerOrder = 10,
            repairGuide = RepairGuideInfo(
                componentKey = "clasp",
                componentName = "Buckle or Clasp",
                problemIdentification = "Weak friction snap, broken safety flip-lock, or missing tang.",
                requiredTools = listOf("1.0 mm pin pusher or jeweler screwdriver", "Spring bar tool"),
                steps = listOf(
                    "Measure the taper width where the strap meets the clasp (often 2–4 mm narrower than lug width).",
                    "Release the clasp spring bar or link screw and transfer the new clasp while keeping directional orientation consistent."
                ),
                difficulty = ReplacementDifficulty.EASY,
                estimatedMinutesRange = "5–10 min",
                safetyWarnings = listOf("Test latch retention 5 times before wearing."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "fasteners",
            name = "Screws & Fasteners",
            alternativeNames = listOf("Bracelet link pins", "Split pins", "Case clamps & screws"),
            primaryFunction = "Join removable bracelet links, retain dial feet, or secure movement clamps inside the case.",
            defaultMaterial = "Hardened Stainless Steel",
            measurementGuidance = "Distinguish threaded link screws vs friction split pins vs pin-and-collar systems, and measure shaft diameter (0.8–1.0 mm).",
            standardSizes = listOf("0.9 mm x 16–20 mm split pin", "1.0 mm bracelet screw"),
            defaultDifficulty = ReplacementDifficulty.EASY,
            layerOrder = 11,
            repairGuide = RepairGuideInfo(
                componentKey = "fasteners",
                componentName = "Screws & Fasteners",
                problemIdentification = "Backed-out link screw, loose bracelet pin, or stripped movement clamp screw.",
                requiredTools = listOf("0.8 mm / 1.0 mm hollow-ground screwdriver", "Link pin block & pusher", "Loctite 222 (low strength)"),
                steps = listOf(
                    "Identify directional arrows engraved on the inside of friction-pin bracelets before pushing.",
                    "For threaded link screws, use an exact-fit hollow-ground blade to avoid burring the slot.",
                    "Apply a pinpoint trace of low-strength threadlocker only to screw threads—never to the link shaft."
                ),
                difficulty = ReplacementDifficulty.EASY,
                estimatedMinutesRange = "10 min",
                safetyWarnings = listOf("Watch for tiny friction collars hidden inside center links of pin-and-collar bracelets."),
                sourceAttribution = "Verified Horological Service Reference"
            )
        )
    )

    val electronicsComponentTemplates: List<CatalogComponentTemplate> = listOf(
        CatalogComponentTemplate(
            key = "display_module",
            name = "OLED / LCD Display Assembly",
            alternativeNames = listOf("Screen assembly", "Digitizer & panel"),
            primaryFunction = "Renders visual output and detects capacitive multi-touch input.",
            defaultMaterial = "Aluminosilicate cover glass + OLED/LCD matrix",
            measurementGuidance = "Verify exact device model number and display connector revision before ordering.",
            standardSizes = listOf("Model-specific flex pinout"),
            defaultDifficulty = ReplacementDifficulty.ADVANCED,
            layerOrder = 0,
            repairGuide = RepairGuideInfo(
                componentKey = "display_module",
                componentName = "Display Assembly",
                problemIdentification = "Cracked cover glass, dead pixels, vertical green lines, or unresponsive touch.",
                requiredTools = listOf("Pentalobe / Torx driver", "Suction cup", "Plastic opening picks", "Heat pad (60°C)"),
                steps = listOf(
                    "Power off the device completely and discharge battery below 25% before opening.",
                    "Soften perimeter display adhesive with controlled 60°C heat and lift carefully with a suction cup.",
                    "Disconnect the battery flex cable FIRST before unplugging display or digitizer connectors."
                ),
                difficulty = ReplacementDifficulty.ADVANCED,
                estimatedMinutesRange = "40–60 min",
                safetyWarnings = listOf("Always disconnect the battery connector before touching display flex cables to prevent shorting the backlight/PMIC."),
                sourceAttribution = "Verified Electronics Repair Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "battery_pack",
            name = "Lithium-Ion Battery Pack",
            alternativeNames = listOf("Internal cell", "Replacement battery"),
            primaryFunction = "Stores and regulates DC power delivery to the logic board.",
            defaultMaterial = "Li-Ion Polymer pouch cell with BMS flex",
            measurementGuidance = "Match exact model code, voltage rating (e.g., 3.85V), and connector footprint.",
            standardSizes = listOf("Model-specific mAh & connector"),
            defaultDifficulty = ReplacementDifficulty.MODERATE,
            layerOrder = 1,
            repairGuide = RepairGuideInfo(
                componentKey = "battery_pack",
                componentName = "Battery Pack",
                problemIdentification = "Rapid battery drain, unexpected shutdowns, or swollen enclosure pushing screen up.",
                requiredTools = listOf("Plastic spudger", "Isopropyl alcohol (90%+)", "Stretch-release adhesive tabs"),
                steps = listOf(
                    "Discharge battery below 25% prior to service.",
                    "Pull stretch-release adhesive tabs slowly at a low angle; if a tab snaps, apply a few drops of isopropyl alcohol under the cell.",
                    "Never pry a glued Li-Ion pouch with a metal screwdriver."
                ),
                difficulty = ReplacementDifficulty.MODERATE,
                estimatedMinutesRange = "25–40 min",
                safetyWarnings = listOf("CRITICAL: Puncturing or bending a Lithium-Ion battery can cause thermal runaway and fire."),
                sourceAttribution = "Verified Electronics Repair Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "charging_port",
            name = "Charging Port / USB-C Flex",
            alternativeNames = listOf("Dock connector", "Daughterboard"),
            primaryFunction = "Handles USB power delivery, data transfer, and often primary microphone routing.",
            defaultMaterial = "Polyimide FPC + Shielded USB-C receptacle",
            measurementGuidance = "Verify exact sub-board revision printed on the PCB.",
            standardSizes = listOf("Model-specific daughterboard"),
            defaultDifficulty = ReplacementDifficulty.MODERATE,
            layerOrder = 2,
            repairGuide = RepairGuideInfo(
                componentKey = "charging_port",
                componentName = "Charging Port / USB-C Flex",
                problemIdentification = "Intermittent cable connection, no fast charging, or bent internal pins.",
                requiredTools = listOf("Non-conductive ESD tweezers", "Wooden toothpick (lint check first)", "Precision screwdriver set"),
                steps = listOf(
                    "First inspect the port with a flashlight and gently clear compacted lint using a non-conductive wooden pick.",
                    "If pins remain damaged, disconnect the battery, unfasten lower speaker bracket, and swap the daughterboard."
                ),
                difficulty = ReplacementDifficulty.MODERATE,
                estimatedMinutesRange = "30–45 min",
                safetyWarnings = listOf("Never insert metal needles inside a live USB-C port."),
                sourceAttribution = "Verified Electronics Repair Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "camera_module",
            name = "Camera Sensor Module",
            alternativeNames = listOf("Main camera unit", "OIS lens module"),
            primaryFunction = "Captures optical imagery with integrated autofocus and optical stabilization.",
            defaultMaterial = "Multi-element glass optics + CMOS sensor + steel shield",
            measurementGuidance = "Confirm exact model and lens position (wide/ultrawide/telephoto).",
            standardSizes = listOf("Model-specific mezzanine connector"),
            defaultDifficulty = ReplacementDifficulty.MODERATE,
            layerOrder = 3,
            repairGuide = RepairGuideInfo(
                componentKey = "camera_module",
                componentName = "Camera Sensor Module",
                problemIdentification = "Unstable OIS jitter, dark spots on sensor, or cracked sapphire lens window.",
                requiredTools = listOf("Anti-static spudger", "Dust-free lens pen"),
                steps = listOf(
                    "Avoid touching exposed optical elements; replace only after disconnecting battery power.",
                    "Seat mezzanine connector flat without forcing."
                ),
                difficulty = ReplacementDifficulty.MODERATE,
                estimatedMinutesRange = "30 min",
                safetyWarnings = listOf("Dust on an open sensor cannot easily be wiped without leaving streaks."),
                sourceAttribution = "Verified Electronics Repair Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "enclosure_fasteners",
            name = "Fasteners & Perimeter Gasket",
            alternativeNames = listOf("Pentalobe screws", "Waterproof seal"),
            primaryFunction = "Secures chassis closure and preserves dust/moisture ingress protection.",
            defaultMaterial = "Stainless micro-screws + die-cut foam adhesive",
            measurementGuidance = "Track screw lengths carefully on a magnetic mat; mixing screw lengths can damage the logic board.",
            standardSizes = listOf("0.8 mm Pentalobe / T2 Torx"),
            defaultDifficulty = ReplacementDifficulty.EASY,
            layerOrder = 4,
            repairGuide = RepairGuideInfo(
                componentKey = "enclosure_fasteners",
                componentName = "Fasteners & Perimeter Gasket",
                problemIdentification = "Lost chassis screws or disturbed factory ingress seal after opening.",
                requiredTools = listOf("Magnetic screw map", "Precision torque driver", "Isopropyl alcohol"),
                steps = listOf(
                    "Remove all remnants of old perimeter adhesive using isopropyl alcohol before applying a fresh die-cut seal.",
                    "Return every screw to its exact original hole length."
                ),
                difficulty = ReplacementDifficulty.EASY,
                estimatedMinutesRange = "15 min",
                safetyWarnings = listOf("Driving a long screw into a short standoff can pierce internal PCB traces (long-screw damage)."),
                sourceAttribution = "Verified Electronics Repair Reference"
            )
        )
    )

    val applianceComponentTemplates: List<CatalogComponentTemplate> = listOf(
        CatalogComponentTemplate(
            key = "group_gasket",
            name = "Group Head Gasket (Seal)",
            alternativeNames = listOf("Portafilter seal", "Brew group O-ring"),
            primaryFunction = "Forms a high-pressure watertight seal (9+ bar) between the brew group and portafilter basket.",
            defaultMaterial = "Food-grade Silicone or EPDM Rubber",
            measurementGuidance = "Measure outer diameter, inner diameter, and thickness (e.g., 73 x 57 x 8.0 mm or 54 mm silicone ring).",
            standardSizes = listOf("54 mm Silicone (Breville/Sage)", "73 x 57 x 8.0 mm (E61)", "72 x 56 x 8.5 mm"),
            defaultDifficulty = ReplacementDifficulty.EASY,
            layerOrder = 0,
            repairGuide = RepairGuideInfo(
                componentKey = "group_gasket",
                componentName = "Group Head Gasket",
                problemIdentification = "Hot water leaking over the portafilter rim during extraction or portafilter locking past 6 o'clock.",
                requiredTools = listOf("Stubby flathead screwdriver or gasket pick", "Group brush", "Food-safe silicone grease (optional)"),
                steps = listOf(
                    "Unplug the espresso machine and allow the brew group to cool completely.",
                    "Unscrew the center dispersion shower screen screw to clear access.",
                    "Hook or pry out the hardened old gasket, scrub the brass/steel groove clean of coffee oils, and press the new silicone gasket bevel-side up."
                ),
                difficulty = ReplacementDifficulty.EASY,
                estimatedMinutesRange = "15 min",
                safetyWarnings = listOf("Wait until boiler and group head are completely cool before servicing."),
                sourceAttribution = "Verified Appliance Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "shower_screen",
            name = "Dispersion Shower Screen",
            alternativeNames = listOf("Diffuser screen", "Brew screen"),
            primaryFunction = "Distributes pressurized brew water evenly across the coffee puck and prevents grounds backflow.",
            defaultMaterial = "304 Stainless Steel micro-mesh",
            measurementGuidance = "Measure outer diameter and center mounting hole vs snap-in lip.",
            standardSizes = listOf("51.5 mm", "54 mm", "58.5 mm / 60 mm"),
            defaultDifficulty = ReplacementDifficulty.EASY,
            layerOrder = 1,
            repairGuide = RepairGuideInfo(
                componentKey = "shower_screen",
                componentName = "Dispersion Shower Screen",
                problemIdentification = "Uneven water channeling, clogged mesh holes, or dented screen mesh.",
                requiredTools = listOf("Stubby Phillips/hex driver", "Cafiza/detergent soak"),
                steps = listOf(
                    "Unscrew the central retaining screw, inspect the dispersion block behind it, and install the new screen snug without over-torquing."
                ),
                difficulty = ReplacementDifficulty.EASY,
                estimatedMinutesRange = "10 min",
                safetyWarnings = listOf("Unplug power before working beneath the brew head."),
                sourceAttribution = "Verified Appliance Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "portafilter_basket",
            name = "Portafilter & Filter Basket",
            alternativeNames = listOf("Filter holder", "18g ridge/ridgeless basket"),
            primaryFunction = "Holds tamped coffee grounds under brewing pressure.",
            defaultMaterial = "Stainless Steel basket + Chrome-plated Brass/Steel handle",
            measurementGuidance = "Measure basket internal diameter (51 mm, 54 mm, or 58 mm) and ear locking tab thickness/count (2-ear vs 3-ear).",
            standardSizes = listOf("51 mm", "54 mm", "58 mm"),
            defaultDifficulty = ReplacementDifficulty.EASY,
            layerOrder = 2,
            repairGuide = RepairGuideInfo(
                componentKey = "portafilter_basket",
                componentName = "Portafilter & Filter Basket",
                problemIdentification = "Clogged pressurized holes, warped rim, or stripped handle thread.",
                requiredTools = listOf("Flat prying edge or blind basket edge"),
                steps = listOf(
                    "Verify diameter (51/54/58 mm) and ear angle before ordering a replacement portafilter or precision basket."
                ),
                difficulty = ReplacementDifficulty.EASY,
                estimatedMinutesRange = "2 min",
                safetyWarnings = listOf("Never unlock a portafilter mid-extraction while under boiler pressure."),
                sourceAttribution = "Verified Appliance Service Reference"
            )
        ),
        CatalogComponentTemplate(
            key = "water_valve_pump",
            name = "Water Reservoir Valve & Pump",
            alternativeNames = listOf("Tank check valve", "Vibration pump"),
            primaryFunction = "Delivers filtered water from the reservoir into the thermoblock/boiler circuit.",
            defaultMaterial = "Food-safe POM polymer, Silicone O-ring, Brass/Ulka pump",
            measurementGuidance = "Verify tank seat O-ring inner/outer diameter or pump wattage/voltage label (e.g., 120V 60Hz / 230V 50Hz).",
            standardSizes = listOf("Model-specific check valve & voltage"),
            defaultDifficulty = ReplacementDifficulty.ADVANCED,
            layerOrder = 3,
            repairGuide = RepairGuideInfo(
                componentKey = "water_valve_pump",
                componentName = "Water Reservoir Valve & Pump",
                problemIdentification = "Pool of water under the tank or pump humming without drawing water.",
                requiredTools = listOf("Security Torx / Phillips driver", "Hose clamp pliers", "Multimeter"),
                steps = listOf(
                    "Check and replace the removable water tank bottom O-ring first before disassembling internal plumbing.",
                    "If internal pump service is required, unplug mains power and drain the reservoir completely."
                ),
                difficulty = ReplacementDifficulty.ADVANCED,
                estimatedMinutesRange = "35–55 min",
                safetyWarnings = listOf("CRITICAL: Disconnect mains AC power before opening appliance chassis; water and AC mains coexist inside."),
                sourceAttribution = "Verified Appliance Service Reference"
            )
        )
    )

    fun templatesForCategory(category: ProductCategory): List<CatalogComponentTemplate> = when (category) {
        ProductCategory.WATCHES -> watchComponentTemplates
        ProductCategory.ELECTRONICS -> electronicsComponentTemplates
        ProductCategory.APPLIANCES -> applianceComponentTemplates
        ProductCategory.OTHER -> emptyList()
    }

    fun findTemplate(category: ProductCategory, categoryKey: String, name: String): CatalogComponentTemplate? {
        val pool = templatesForCategory(category) + watchComponentTemplates + electronicsComponentTemplates + applianceComponentTemplates
        return pool.firstOrNull { it.key.equals(categoryKey, ignoreCase = true) }
            ?: pool.firstOrNull {
                name.contains(it.name, ignoreCase = true) ||
                    it.name.contains(name, ignoreCase = true) ||
                    it.alternativeNames.any { alt -> name.contains(alt, ignoreCase = true) }
            }
    }

    fun getRepairGuide(category: ProductCategory, categoryKey: String, componentName: String): RepairGuideInfo {
        val template = findTemplate(category, categoryKey, componentName)
        if (template != null) return template.repairGuide

        return RepairGuideInfo(
            componentKey = categoryKey,
            componentName = componentName,
            problemIdentification = "Inspect $componentName for mechanical wear, loose fasteners, or material fatigue.",
            requiredTools = listOf("Digital calipers", "Precision screwdriver set", "Clean microfiber work mat"),
            steps = listOf(
                "Document the assembly orientation with photographs before loosening any fasteners.",
                "Measure critical mounting dimensions with calipers before ordering replacement parts.",
                "Verify fitment without forcing fasteners during reassembly."
            ),
            difficulty = ReplacementDifficulty.MODERATE,
            estimatedMinutesRange = "Variable (unverified assembly)",
            safetyWarnings = listOf(
                "Disconnect power or release stored spring/pressure energy before disassembly.",
                "No manufacturer-specific repair manual is loaded for this item yet."
            ),
            sourceAttribution = "General Safety & Measurement Guidance (Unverified Assembly)"
        )
    }

    fun buildSearchChannels(
        brand: String,
        model: String,
        category: ProductCategory,
        component: ScanComponentDoc
    ): List<MarketplaceSearchChannel> {
        val cleanBrand = brand.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) } ?: ""
        val cleanModel = model.takeIf { it.isNotBlank() && !it.equals("Unidentified Product", ignoreCase = true) && !it.equals("Unknown", ignoreCase = true) } ?: ""
        val partQuery = listOf(cleanBrand, cleanModel, component.name, component.dimensions.takeIf { it.isNotBlank() } ?: "")
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val encoded = Uri.encode(partQuery)
        val status = CompatibilityStatus.fromRaw(component.compatibilityStatus)

        return when (category) {
            ProductCategory.WATCHES -> listOf(
                MarketplaceSearchChannel(
                    id = "esslinger",
                    partnerName = "Esslinger Watch Parts",
                    channelType = "Authorized Horological Supplier Search",
                    searchQuery = partQuery,
                    searchUrl = "https://www.esslinger.com/search.php?search_query=$encoded",
                    specNotes = "Filter by exact lug width / caliber / tap size before ordering.",
                    originalOrAftermarket = "OEM & Swiss Aftermarket",
                    materialSpec = component.material.ifBlank { "316L Steel / Mineral / Sapphire" },
                    dimensionSpec = component.dimensions.ifBlank { "Measure with 0.01 mm calipers" },
                    compatibilityStatus = status
                ),
                MarketplaceSearchChannel(
                    id = "cousins_uk",
                    partnerName = "Cousins UK Material House",
                    channelType = "Technical Watchmaker Catalog Search",
                    searchQuery = partQuery,
                    searchUrl = "https://www.cousinsuk.com/search?searchTerm=$encoded",
                    specNotes = "Provides technical datasheet cross-references by caliber and case reference.",
                    originalOrAftermarket = "Original OEM & Technical Spec",
                    materialSpec = component.material.ifBlank { "Manufacturer-grade" },
                    dimensionSpec = component.dimensions.ifBlank { "Check case reference number" },
                    compatibilityStatus = status
                ),
                MarketplaceSearchChannel(
                    id = "chrono24_parts",
                    partnerName = "Chrono24 Parts & Accessories",
                    channelType = "Verified Marketplace Search",
                    searchQuery = partQuery,
                    searchUrl = "https://www.chrono24.com/search/index.htm?query=$encoded",
                    specNotes = "Compare seller authenticity guarantees for branded clasps, bezels, and straps.",
                    originalOrAftermarket = "Original Branded Parts",
                    materialSpec = component.material.ifBlank { "Brand-matched finish" },
                    dimensionSpec = component.dimensions.ifBlank { "Confirm reference match" },
                    compatibilityStatus = status
                )
            )
            ProductCategory.ELECTRONICS -> listOf(
                MarketplaceSearchChannel(
                    id = "ifixit",
                    partnerName = "iFixit Parts Store",
                    channelType = "Verified Repair Parts & Guides",
                    searchQuery = partQuery,
                    searchUrl = "https://www.ifixit.com/Search?query=$encoded",
                    specNotes = "Includes paired teardowns, adhesive kits, and exact model compatibility notes.",
                    originalOrAftermarket = "OEM & Tested Grade",
                    materialSpec = component.material.ifBlank { "Factory spec assembly" },
                    dimensionSpec = component.dimensions.ifBlank { "Match device model ID" },
                    compatibilityStatus = status
                ),
                MarketplaceSearchChannel(
                    id = "mobilesentrix",
                    partnerName = "MobileSentrix Catalog Search",
                    channelType = "Component Wholesaler Search",
                    searchQuery = partQuery,
                    searchUrl = "https://www.mobilesentrix.com/catalogsearch/result/?q=$encoded",
                    specNotes = "Verify flex connector revision before installation.",
                    originalOrAftermarket = "OEM Pull & Aftermarket Tiers",
                    materialSpec = component.material.ifBlank { "Original-grade assembly" },
                    dimensionSpec = component.dimensions.ifBlank { "Confirm A-number / SKU" },
                    compatibilityStatus = status
                )
            )
            ProductCategory.APPLIANCES -> listOf(
                MarketplaceSearchChannel(
                    id = "repairclinic",
                    partnerName = "RepairClinic OEM Parts",
                    channelType = "Verified Appliance Parts Lookup",
                    searchQuery = partQuery,
                    searchUrl = "https://www.repairclinic.com/Shop-For-Parts?query=$encoded",
                    specNotes = "Cross-check rating plate model number inside appliance housing.",
                    originalOrAftermarket = "Genuine OEM",
                    materialSpec = component.material.ifBlank { "Food-safe / Thermal-rated" },
                    dimensionSpec = component.dimensions.ifBlank { "Check rating plate model" },
                    compatibilityStatus = status
                ),
                MarketplaceSearchChannel(
                    id = "ereplacementparts",
                    partnerName = "eReplacementParts Diagrams",
                    channelType = "Exploded Diagram & OEM Search",
                    searchQuery = partQuery,
                    searchUrl = "https://www.ereplacementparts.com/search_result.php?q=$encoded",
                    specNotes = "Matches manufacturer exploded schematics by serial/type number.",
                    originalOrAftermarket = "Genuine OEM",
                    materialSpec = component.material.ifBlank { "OEM Specification" },
                    dimensionSpec = component.dimensions.ifBlank { "Confirm OD/ID dimensions" },
                    compatibilityStatus = status
                )
            )
            ProductCategory.OTHER -> listOf(
                MarketplaceSearchChannel(
                    id = "mcmaster",
                    partnerName = "McMaster-Carr Precision Hardware",
                    channelType = "Dimensional Hardware Search",
                    searchQuery = component.name,
                    searchUrl = "https://www.mcmaster.com/${Uri.encode(component.name)}",
                    specNotes = "Best when matching exact measured fasteners, O-rings, or seals.",
                    originalOrAftermarket = "Industrial Standard",
                    materialSpec = component.material.ifBlank { "Stainless / Silicone / Viton" },
                    dimensionSpec = component.dimensions.ifBlank { "Measure OD, ID, thread pitch" },
                    compatibilityStatus = status
                )
            )
        }
    }

    /**
     * Clearly-labeled demonstration sample format showing how partner API pricing & stock renders
     * once live e-commerce partner credentials are configured. Never presented as live real inventory.
     */
    fun getSampleDemoListings(component: ScanComponentDoc): List<MarketplaceSampleListing> {
        val status = CompatibilityStatus.fromRaw(component.compatibilityStatus)
        val dim = component.dimensions.ifBlank { "Standard measured fit" }
        val mat = component.material.ifBlank { "316L Stainless / Spec Material" }
        return listOf(
            MarketplaceSampleListing(
                id = "demo_oem_1",
                title = "OEM Spec ${component.name} ($dim)",
                sellerName = "Demo Partner • Horology Supply",
                priceUsd = 24.50,
                availabilityLabel = "Sample Format • In Stock",
                deliveryEstimate = "2–4 business days",
                material = mat,
                dimensions = dim,
                isOriginalOem = true,
                isRefurbished = false,
                compatibilityStatus = status,
                sellerRating = 4.9
            ),
            MarketplaceSampleListing(
                id = "demo_aftermarket_2",
                title = "Precision Aftermarket ${component.name}",
                sellerName = "Demo Partner • Precision Parts Co.",
                priceUsd = 11.90,
                availabilityLabel = "Sample Format • Low Stock",
                deliveryEstimate = "3–6 business days",
                material = mat,
                dimensions = dim,
                isOriginalOem = false,
                isRefurbished = false,
                compatibilityStatus = if (status == CompatibilityStatus.VERIFIED_COMPATIBLE) CompatibilityStatus.LIKELY_COMPATIBLE else status,
                sellerRating = 4.6
            ),
            MarketplaceSampleListing(
                id = "demo_refurb_3",
                title = "Tested Pulled ${component.name}",
                sellerName = "Demo Partner • Circular Repair Hub",
                priceUsd = 16.00,
                availabilityLabel = "Sample Format • 1 unit",
                deliveryEstimate = "4–7 business days",
                material = mat,
                dimensions = dim,
                isOriginalOem = true,
                isRefurbished = true,
                compatibilityStatus = CompatibilityStatus.LIKELY_COMPATIBLE,
                sellerRating = 4.7
            )
        )
    }

    fun createVerifiedWatchDemoBundle(userId: String): ScanResultBundle {
        val scan = ScanDoc(
            userId = userId,
            productBrand = "Seiko (Reference Assembly)",
            productModel = "SKX007 Automatic Diver 200m",
            category = ProductCategory.WATCHES.firestoreValue,
            generationOrEra = "1996–2019 (Caliber 7S26)",
            movementOrPowerType = "Automatic Mechanical (21,600 vph)",
            confidenceScore = 0.96,
            confidenceLabel = ConfidenceLevel.HIGH.name,
            damageNotes = "Reference assembly loaded from verified dataset — no user photo damage inspected.",
            missingComponentNotes = "All 12 standard mechanical watch sub-assemblies included.",
            needsAdditionalAngle = false,
            suggestedNextAngle = "",
            dataVerificationStatus = VerificationProvenance.VERIFIED_DATASET.name,
            componentCount = watchComponentTemplates.size.toLong()
        )

        val components = watchComponentTemplates.mapIndexed { idx, tpl ->
            val dim = when (tpl.key) {
                "watch_crystal" -> "31.5 mm dia x 3.2 mm Hardlex"
                "bezel" -> "38.0 mm OD x 31.5 mm ID insert"
                "dial" -> "28.5 mm dia (3.8 o'clock date)"
                "hands" -> "1.50 / 0.89 / 0.21 mm pinions"
                "crown" -> "7.0 mm screw-down • Tap 10 stem"
                "watch_case" -> "42.5 mm dia • 22 mm lug width"
                "movement" -> "27.4 mm dia • Cal. 7S26 / NH36"
                "case_back" -> "31.5 mm screw-down + 0.6 mm O-ring"
                "strap" -> "22 mm lug width -> 20 mm taper"
                "spring_bar" -> "22 mm x 2.5 mm (1.1 mm dive tips)"
                "clasp" -> "20 mm dual-push deployant clasp"
                else -> "1.0 mm link pins & collar system"
            }
            val partNum = when (tpl.key) {
                "watch_crystal" -> "315P05HN02"
                "spring_bar" -> "C220FS (Fat 2.5mm)"
                "movement" -> "7S26C / NH36A"
                "case_back" -> "0C3060B01 Gasket"
                else -> "Verify exact case stamp"
            }
            ScanComponentDoc(
                userId = userId,
                scanId = "",
                name = tpl.name,
                categoryKey = tpl.key,
                alternativeNames = tpl.alternativeNames,
                primaryFunction = tpl.primaryFunction,
                material = tpl.defaultMaterial,
                dimensions = dim,
                partNumber = partNum,
                compatibilityStatus = CompatibilityStatus.VERIFIED_COMPATIBLE.name,
                compatibilityReason = "Matched against verified Seiko SKX007 / 7S26 architectural specifications.",
                replacementDifficulty = tpl.defaultDifficulty.name,
                visibleInPhoto = idx in listOf(0, 1, 2, 3, 4, 5, 8, 10),
                hiddenInViewer = false,
                layerOrder = idx.toLong()
            )
        }
        return ScanResultBundle(scan = scan, components = components)
    }

    fun createVerifiedCategoryBundle(userId: String, category: ProductCategory): ScanResultBundle {
        if (category == ProductCategory.WATCHES) return createVerifiedWatchDemoBundle(userId)
        val templates = templatesForCategory(category)
        val (brand, model, power) = when (category) {
            ProductCategory.ELECTRONICS -> Triple("Generic Modular Smartphone", "Reference OLED Handset", "3.85V Li-Ion Polymer")
            ProductCategory.APPLIANCES -> Triple("Reference Espresso Machine", "54mm / 58mm Semi-Auto Group", "1200W Thermoblock + 15-bar Pump")
            else -> Triple("Reference Assembly", "General Hardware", "Mechanical")
        }
        val scan = ScanDoc(
            userId = userId,
            productBrand = brand,
            productModel = model,
            category = category.firestoreValue,
            generationOrEra = "Standard Modular Architecture",
            movementOrPowerType = power,
            confidenceScore = 0.90,
            confidenceLabel = ConfidenceLevel.HIGH.name,
            damageNotes = "Reference assembly loaded from verified category dataset.",
            missingComponentNotes = "None",
            needsAdditionalAngle = false,
            suggestedNextAngle = "",
            dataVerificationStatus = VerificationProvenance.VERIFIED_DATASET.name,
            componentCount = templates.size.toLong()
        )
        val components = templates.mapIndexed { idx, tpl ->
            ScanComponentDoc(
                userId = userId,
                scanId = "",
                name = tpl.name,
                categoryKey = tpl.key,
                alternativeNames = tpl.alternativeNames,
                primaryFunction = tpl.primaryFunction,
                material = tpl.defaultMaterial,
                dimensions = tpl.standardSizes.firstOrNull() ?: "Verify model label",
                partNumber = "Verify device rating plate",
                compatibilityStatus = CompatibilityStatus.LIKELY_COMPATIBLE.name,
                compatibilityReason = "Representative component for ${category.displayName}; verify model number before ordering.",
                replacementDifficulty = tpl.defaultDifficulty.name,
                visibleInPhoto = true,
                hiddenInViewer = false,
                layerOrder = idx.toLong()
            )
        }
        return ScanResultBundle(scan = scan, components = components)
    }
}
