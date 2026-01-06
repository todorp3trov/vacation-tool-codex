import type { AppProps } from "next/app";
import "../styles/login.css";
import "../styles/dashboard.css";
import "../styles/vacation-dialog.css";

export default function App({ Component, pageProps }: AppProps) {
  return <Component {...pageProps} />;
}
