import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import http from "http";
import db from "./config/db.js";
import authRoutes from "./routes/authRoutes.js";

dotenv.config();

const app = express();
const server = http.createServer(app);



app.use(cors());
app.use(express.json());

app.get("/", (req, res) => {
  res.json({ message: "TiffinCraft backend is running" });
});


app.use("/api/auth", authRoutes);



const PORT = process.env.PORT || 5000;
server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});