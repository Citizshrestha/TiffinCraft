
  import { createRoot } from "react-dom/client";
  import App from "./app/App";
  import "./styles/index.css";
  import { ToastProvider } from "./app/components/Toast";

  createRoot(document.getElementById("root")!).render(
    <ToastProvider>
      <App />
    </ToastProvider>
  );
  