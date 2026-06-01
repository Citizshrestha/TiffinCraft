import db from "../config/db";
import bcrypt from "bcrypt";

export const registerUser = async (req, res) => {
	try {
		const {name, email, password, role} = req.body;

		if (!name || !email || !password){
			return res.status(400).json({
				message: "All fields are required.",
			});
		}

		const [registeredUser] = await db.query(
			"SELECT * FROM users WHERE email = ?",
			[email]
		);

		if (registeredUser.length > 0) {
			return res.status(400).json({
				message: "User already exists.",
			});
		}

		const hashPass = await bcrypt.hash(password, 10);

		await db.query(
			"INSERT INTO users(name, email, password, role) VALUES (?,?,?,?)",
			[name, email, hashPass, role || "New User"]
		);
		return res.status(201).json({
			message: "User registered successfully.",
		})
	} catch (error) {
		res.status(500).json({ message: "Server error", error: err.message });
	}
};

export const loginUser = async (req, res) => {
	res.status(501).json({
		message: "Login endpoint not implemented yet.",
	});
};

export const getCurrentUser = async (req, res) => {
	res.status(501).json({
		message: "Current user endpoint not implemented yet.",
	});
};
