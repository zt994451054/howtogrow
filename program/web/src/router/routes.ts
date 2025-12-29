import type { RouteRecordRaw } from "vue-router";
import AdminLayout from "@/layouts/AdminLayout.vue";
import LoginPage from "@/pages/LoginPage.vue";
import NotFoundPage from "@/pages/NotFoundPage.vue";
import ForbiddenPage from "@/pages/ForbiddenPage.vue";
import QuestionsPage from "@/pages/QuestionsPage.vue";
import PlansPage from "@/pages/PlansPage.vue";
import QuotesPage from "@/pages/QuotesPage.vue";
import UsersPage from "@/pages/UsersPage.vue";
import OrdersPage from "@/pages/OrdersPage.vue";
import AssessmentsPage from "@/pages/AssessmentsPage.vue";
import PermissionsPage from "@/pages/rbac/PermissionsPage.vue";
import RolesPage from "@/pages/rbac/RolesPage.vue";
import AdminUsersPage from "@/pages/rbac/AdminUsersPage.vue";

export const routes: RouteRecordRaw[] = [
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
        path: "users",
        name: "users",
        component: UsersPage,
        meta: { requiresAuth: true, permission: "USER:READ", title: "用户" }
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
