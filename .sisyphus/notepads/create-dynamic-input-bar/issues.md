# Issues

- 2026-05-20: `DynamicInputBar.kt` 自身无编译错误，但 `./gradlew :app:compileDebugKotlin` 因 `AiBookkeepingScreen.kt` 的预存在问题（ViewModel 未创建、Icons 依赖未解析）而失败。该问题在创建此组件前已经存在，非本组件引入。
