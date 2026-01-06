import Head from "next/head";
import LoginForm from "../components/LoginForm";

export default function LoginPage() {
  return (
    <>
      <Head>
        <title>Vacation Tool | Login</title>
      </Head>
      <div className="login-page">
        <div className="login-card">
          <LoginForm />
        </div>
      </div>
    </>
  );
}
