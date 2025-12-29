import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import { createPinia } from "pinia";
import App from "@/App.vue";
import { router } from "@/router";
import { vPermission } from "@/directives/permission";

import "@/styles/global.css";

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(ElementPlus);

app.directive("permission", vPermission);

app.mount("#app");

