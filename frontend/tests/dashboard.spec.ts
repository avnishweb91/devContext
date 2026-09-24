import { expect, test } from "@playwright/test";

test("shows the engineering dashboard", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Good morning, Avnish" })).toBeVisible();
  await expect(page.getByText("Pull request verification")).toBeVisible();
  await expect(page.getByText("Automation opportunities")).toBeVisible();
});

test("opens pull request verification evidence", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: /Add rate-limit handling/ }).click();
  await expect(page.getByRole("heading", { name: "Verification summary" })).toBeVisible();
  await expect(page.getByText("Evidence findings")).toBeVisible();
  await expect(page.getByText("GitHub pull request")).toBeVisible();
});

test("renders the team invitation workflow", async ({ page }) => {
  await page.goto("/team");
  await expect(page.getByRole("heading", { name: "Invite your team" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Create invitation" })).toBeVisible();
});

test("renders the invite acceptance workflow", async ({ page }) => {
  await page.goto("/accept-invite?token=test-token");
  await expect(page.getByRole("heading", { name: "Accept your invitation" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Accept invitation" })).toBeVisible();
});

test("renders the engineering decisions workflow", async ({ page }) => {
  await page.goto("/decisions");
  await expect(page.getByRole("heading", { name: "Decisions your team can trust" })).toBeVisible();
  await expect(page.getByPlaceholder("Decision title")).toBeVisible();
  await expect(page.getByRole("button", { name: "Save decision" })).toBeVisible();
});

test("renders the pull request review history surface", async ({ page }) => {
  await page.goto("/pull-requests");
  await expect(page.getByRole("heading", { name: "Pull requests" })).toBeVisible();
  await expect(page.getByText("Review repository changes, record evidence, and keep each workspace isolated.")).toBeVisible();
});

test("retries a failed GitHub synchronization", async ({ page }) => {
  await page.route("**/api/workspaces", route => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([{ id: "workspace-1", name: "Acme" }]) }));
  await page.route("**/api/integrations/providers", route => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([{ id: "github", name: "GitHub", authorizationPath: "/oauth2/authorization/github" }]) }));
  await page.route("**/api/integrations/github/workspaces/workspace-1/sync", route => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ jobId: "job-1", repositories: 0, pullRequests: 0, status: "PENDING" }) }));
  await page.route("**/api/integrations/github/workspaces/workspace-1/sync-jobs/job-1", route => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ jobId: "job-1", repositories: 0, pullRequests: 0, status: "FAILED", errorMessage: "GitHub rate limit" }) }));
  await page.route("**/api/integrations/github/workspaces/workspace-1/sync-jobs/job-1/retry", route => route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ jobId: "job-2", repositories: 0, pullRequests: 0, status: "PENDING" }) }));

  await page.goto("/integrations");
  await page.getByRole("button", { name: "Sync GitHub repositories" }).click();
  await expect(page.getByText("GitHub sync failed: GitHub rate limit")).toBeVisible();
  await page.getByRole("button", { name: "Retry failed sync" }).click();
  await expect(page.getByText("GitHub retry queued. Run sync again shortly to read its status.")).toBeVisible();
});
