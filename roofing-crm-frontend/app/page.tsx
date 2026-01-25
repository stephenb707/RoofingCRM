import { redirect } from "next/navigation";

export default function HomePage() {
  // Redirect to auth login page
  redirect("/auth/login");
}
