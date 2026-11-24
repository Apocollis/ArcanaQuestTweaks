package com.apocollis.aqtweaks.starter;

/** The settings written on first run: the prompt, the six classes and their memory texts (plan 13.1, 13.4, 13.5). */
public final class StarterDefaults {

    private StarterDefaults() {}

    public static final String INTRO = "You wake to an untamed world, far from the sanctuary where you last drew breath. "
            + "Centuries have passed since the fall, your soul anchor is shattered, and most of what your order built "
            + "has crumbled into ruin. The details of the cataclysm remain lost in the dark—all save for a single, "
            + "stubborn memory of who you were when the end began.";

    public static final String PROMPT = "What's the last thing you remember?";

    /** Shown instead of {@link #INTRO} when a Tabula Rasa draught reopens the menu. */
    public static final String RESET_INTRO = "Your mind tingles as the draught settles like cool starlight, wiping the "
            + "slate of your training clean and leaving you brimming with untamed potential. Everything you have "
            + "survived and learned along this journey remains yours. However, your fate stands unwritten once more. "
            + "Glimpses of lifetimes past echo through your mind yet again.";

    public static final String RESET_PROMPT = "Which memory defines who you were when the end began?";

    public static StarterSettings create() {
        StarterSettings s = new StarterSettings();
        s.intro = INTRO;
        s.prompt = PROMPT;
        s.resetIntro = RESET_INTRO;
        s.resetPrompt = RESET_PROMPT;
        s.showDetails = false;
        s.revealHoldSeconds = 12;
        s.classes.add(new StarterClassDef("warrior", "Warrior",
                "I stood in the breach of a crumbling keep as the wave came on, steel ringing against my shield until "
                        + "the last of them lay still. I never learned who they were or why their banners looked so "
                        + "familiar. Somewhere beyond the horns, something older was watching and waiting. As my soul "
                        + "anchor was broken and the dark took me, I remember thinking the fight was only the beginning.",
                "tex:aqtweaks:class_warrior", "warrior", "aqtweaks:riposte")
                .skill("reskillable:attack", 4).skill("reskillable:defense", 4));
        s.classes.add(new StarterClassDef("archer", "Archer",
                "I hunted beast and enemy alike from the treeline, a bowstring drawn to my cheek. Speed and a steady "
                        + "eye were all I ever trusted and I could tell a deer's path from a raider's by the silence it "
                        + "left behind. Then one night the forest went quiet in a way I had never heard, and the stars "
                        + "above the canopy seemed to dim. Whatever was coming wasn't just raiders, and I loosed my "
                        + "last arrow into the dark before the sleep took me.",
                "tex:aqtweaks:class_archer", "archer", "aqtweaks:steady_draw")
                .skill("reskillable:attack", 4).skill("reskillable:agility", 4));
        s.classes.add(new StarterClassDef("scout", "Scout",
                "I traveled mountain to sea, and crept through quiet halls where the only sound was my own breathing. "
                        + "I made it home on stealth, wit, and skill. Deep beneath one of our own keeps, I found a "
                        + "sealed gateway that wasn't on any chart, locked with keys that didn't belong to our order. "
                        + "I never figured out how to open it before the world fell apart, and I've wondered ever "
                        + "since what was waiting on the other side.",
                "tex:aqtweaks:class_scout", "scout", "aqtweaks:fleetfoot")
                .skill("reskillable:agility", 4).skill("reskillable:attack", 2).skill("reskillable:gathering", 2));
        s.classes.add(new StarterClassDef("miner", "Miner",
                "I was at home in the deep places, where my pick rang against stone and ember-lit veins. I drew ores "
                        + "out of the dark that no one else had the patience to reach—every seam felt like a quiet "
                        + "secret the earth had decided to share. Then one day my pick broke into a hollow older than "
                        + "the stone itself, and I felt something in the dark listening back.",
                "tex:aqtweaks:class_miner", "miner", "aqtweaks:vein_rhythm")
                .skill("reskillable:mining", 4).skill("reskillable:defense", 4));
        s.classes.add(new StarterClassDef("homesteader", "Homesteader",
                "I built a hearth and a sanctuary to come back to after every long journey—a steady anchor "
                        + "against the wilds. Keeping a warm fire and an open seat for anyone who needed rest was the "
                        + "part of our calling I loved most. The last night I lit it, the roads outside had gone empty "
                        + "and the ward-stones were cold. I knew then I wouldn't see that fire again for a very long time.",
                "tex:aqtweaks:class_homesteader", "homesteader", "aqtweaks:hearthkeeper")
                .skill("reskillable:farming", 4).skill("reskillable:gathering", 2).skill("reskillable:building", 2));
        s.classes.add(new StarterClassDef("scholar", "Scholar",
                "I lost whole nights in the archives by candlelight, tracing the threads of Vis and starlight for one "
                        + "more secret of the realms. The answers were always one page further on, but the mystery "
                        + "itself is what drove me on. Then one night I uncovered a passage with no author and no end - "
                        + "only warnings of a fracture opening in the dark. It was the last thing I read before the "
                        + "sleep took me.",
                "tex:aqtweaks:class_scholar", "scholar", "aqtweaks:studious")
                .skill("reskillable:magic", 4).skill("reskillable:gathering", 2).skill("reskillable:mining", 2)
                .command("tc research %player% add FIRSTSTEPS"));
        return s;
    }
}
