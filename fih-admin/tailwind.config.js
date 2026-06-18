/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {
      colors: {
        primary: "#1f5f8b",
        "primary-700": "#0d2b40",
        accent: "#0f9d9d",
        ink: "#1a2230",
        muted: "#5b6470",
        bg: "#f4f7fa",
        surface: "#ffffff",
        line: "#e2e6ec",
        success: "#0a7c4a",
        warn: "#b54708"
      },
      fontFamily: { sans: ["Inter", "system-ui", "sans-serif"] },
      borderRadius: { card: "16px" },
      boxShadow: { card: "0 1px 3px rgba(16,30,54,.08)", "card-hover": "0 6px 20px rgba(16,30,54,.12)" }
    }
  },
  plugins: []
};
