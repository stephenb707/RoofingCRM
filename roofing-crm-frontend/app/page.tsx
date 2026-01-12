import { redirect } from "next/navigation";

export default function HomePage() {
  // For now, always redirect to /login
  redirect("/login");
}
