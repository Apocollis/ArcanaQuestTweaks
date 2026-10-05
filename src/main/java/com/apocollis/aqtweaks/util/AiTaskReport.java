package com.apocollis.aqtweaks.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.ai.EntityAIBase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Log-once reporting for AI tasks that throw inside {@code MixinEntityAITasks}. */
public final class AiTaskReport {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-AI");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private AiTaskReport() {}

    public static void npe(EntityAIBase task, NullPointerException e) {
        String name = task.getClass().getName();
        if (REPORTED.add(name)) {
            LOGGER.warn("[AQ-AI] {} threw NullPointerException in updateTask; task reset (logged once per class)", name, e);
        }
    }

    public static void resetFailed(EntityAIBase task, RuntimeException e) {
        String name = task.getClass().getName();
        if (REPORTED.add(name + "#reset")) {
            LOGGER.warn("[AQ-AI] {} also failed in resetTask (logged once per class)", name, e);
        }
    }
}
