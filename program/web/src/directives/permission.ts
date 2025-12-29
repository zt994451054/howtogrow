import type { Directive } from "vue";
import { usePermissionStore } from "@/stores/permission";

export const vPermission: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    const permissionCode = binding.value;
    const permission = usePermissionStore();
    if (!permissionCode) return;
    if (!permission.hasPermission(permissionCode)) {
      el.style.display = "none";
    }
  }
};

