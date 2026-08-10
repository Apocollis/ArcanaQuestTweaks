\### CleanroomMC 1.12.2 Modding Profile



\### Environment \& Toolchain



\* \*\*Game Version\*\*: Minecraft 1.12.2 (Forge Ecosystem)

\* \*\*Runtime\*\*: CleanroomMC (Runs on modern Java 21)

\* \*\*Mappings\*\*: MCP stable\_39 / SRG names for production code

\* \*\*Build System\*\*: Gradle (managed by Antigravity)



\### Code Generation \& API Strict Constraints



\* \*\*Java Language Level\*\*: Use modern Java 21 features (e.g., var, text blocks, switch expressions, records) where applicable.

\* \*\*Mod Loading\*\*: Use standard @Mod annotations for 1.12.2.

\* \*\*Registries\*\*: Strictly use RegistryEvent.Register<T> events. Do \*\*NOT\*\* use RegistryObject or DeferredRegister (which are 1.16+ features).

\* \*\*Items \& Blocks\*\*: Use setRegistryName() and setTranslationKey(). Do \*\*NOT\*\* use modern Identifier or ResourceLocation namespace constructor styles that differ from 1.12.2.

\* \*\*Proxies\*\*: Adhere to the traditional @SidedProxy pattern with CommonProxy and ClientProxy if needed, though prefer Cleanroom-native event loading where possible.



\### Mixin Guidelines (SpongePowered Mixin)



\* \*\*Targeting\*\*: When writing @Mixin, use SRG names (func\_... for methods, field\_... for fields) in the method or target strings to ensure production compatibility.

\* \*\*Injectors\*\*: Prefer @Inject with at = @At("HEAD") or at = @At("RETURN"). Use @Redirect and @Overwrite sparingly and only when documented.

\* \*\*Shadowing\*\*: Use @Shadow with the correct SRG mapping to access private/protected vanilla members.

\* \*\*Cleanroom Features\*\*: Leverage MixinBooter / Fugue integration capabilities implicitly.



\### Project Structure \& Architecture



\* Code lives in src/main/java.

\* Assets live in src/main/resources/assets/\[modid]/.

\* Traditional 1.12.2 JSON structures required for blockstates, models, and recipes.



\### Memory \& Execution Optimization



\* Minimize object allocation in heavy tick events (RenderGameOverlayEvent, LivingUpdateEvent).

\* Ensure packets utilize SimpleNetworkWrapper and are registered on the correct sides.

