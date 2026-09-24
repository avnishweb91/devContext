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

