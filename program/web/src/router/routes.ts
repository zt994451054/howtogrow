import type { RouteRecordRaw } from "vue-router";
import AdminLayout from "@/layouts/AdminLayout.vue";
import LoginPage from "@/pages/LoginPage.vue";
import NotFoundPage from "@/pages/NotFoundPage.vue";
import ForbiddenPage from "@/pages/ForbiddenPage.vue";
import QuestionsPage from "@/pages/QuestionsPage.vue";
import PlansPage from "@/pages/PlansPage.vue";
import QuotesPage from "@/pages/QuotesPage.vue";
import BannersPage from "@/pages/BannersPage.vue";
import TroubleScenesPage from "@/pages/TroubleScenesPage.vue";
import AiQuickQuestionsPage from "@/pages/AiQuickQuestionsPage.vue";
import UsersPage from "@/pages/UsersPage.vue";
import ChildrenPage from "@/pages/ChildrenPage.vue";
import OrdersPage from "@/pages/OrdersPage.vue";
import AssessmentsPage from "@/pages/AssessmentsPage.vue";
import PermissionsPage from "@/pages/rbac/PermissionsPage.vue";
import RolesPage from "@/pages/rbac/RolesPage.vue";
import AdminUsersPage from "@/pages/rbac/AdminUsersPage.vue";
import H5BannerPage from "@/pages/h5/H5BannerPage.vue";

export const routes: RouteRecordRaw[] = [
  {
    path: "/h5/banners/:id",
    name: "h5-banner",
    component: H5BannerPage,
    meta: { requiresAuth: false, title: "Banner" }
  },
  {
    path: "/login",
    name: "login",
    component: LoginPage,
    meta: { requiresAuth: false, title: "登录" }
  },
  {
    path: "/",
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      { path: "", redirect: "/questions" },
      {
        path: "403",
        name: "forbidden",
        component: ForbiddenPage,
        meta: { requiresAuth: true, title: "无权限" }
      },
      {
        path: "questions",
        name: "questions",
        component: QuestionsPage,
        meta: { requiresAuth: true, permission: "QUESTION:MANAGE", title: "题库" }
      },
      {
        path: "plans",
        name: "plans",
        component: PlansPage,
        meta: { requiresAuth: true, permission: "PLAN:MANAGE", title: "套餐" }
      },
      {
        path: "quotes",
        name: "quotes",
        component: QuotesPage,
        meta: { requiresAuth: true, permission: "QUOTE:MANAGE", title: "鸡汤语" }
      },
      {
        path: "banners",
        name: "banners",
        component: BannersPage,
        meta: { requiresAuth: true, permission: "QUOTE:MANAGE", title: "Banner" }
      },
      {
        path: "users",
        name: "users",
        component: UsersPage,
        meta: { requiresAuth: true, permission: "USER:READ", title: "用户" }
      },
      {
        path: "children",
        name: "children",
        component: ChildrenPage,
        meta: { requiresAuth: true, permission: "USER:READ", title: "孩子" }
      },
      {
        path: "orders",
        name: "orders",
        component: OrdersPage,
        meta: { requiresAuth: true, permission: "ORDER:READ", title: "订单" }
      },
      {
        path: "assessments",
        name: "assessments",
        component: AssessmentsPage,
        meta: { requiresAuth: true, permission: "ASSESSMENT:READ", title: "自测" }
      },
      {
        path: "trouble-scenes",
        name: "trouble-scenes",
        component: TroubleScenesPage,
        meta: { requiresAuth: true, permission: "QUESTION:MANAGE", title: "烦恼场景" }
      },
      {
        path: "ai-quick-questions",
        name: "ai-quick-questions",
        component: AiQuickQuestionsPage,
        meta: { requiresAuth: true, permission: "AI_QUICK_QUESTION:MANAGE", title: "快捷问题" }
      },
      {
        path: "admin/users",
        name: "admin-users",
        component: AdminUsersPage,
        meta: { requiresAuth: true, permission: "RBAC:MANAGE", title: "管理员" }
      },
      {
        path: "admin/roles",
        name: "admin-roles",
        component: RolesPage,
        meta: { requiresAuth: true, permission: "RBAC:MANAGE", title: "角色" }
      },
      {
        path: "admin/permissions",
        name: "admin-permissions",
        component: PermissionsPage,
        meta: { requiresAuth: true, permission: "RBAC:MANAGE", title: "权限" }
      }
    ]
  },
  { path: "/:pathMatch(.*)*", name: "not-found", component: NotFoundPage }
];
