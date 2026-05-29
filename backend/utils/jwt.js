import jwt from "jwt";

const generateToken = (id, role) => {
    return jwt.sign({ id , role }, process.env.JWT_SECRET, {expiresIn: "7d"});
};

export {generateToken};