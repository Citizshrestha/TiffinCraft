// Self-check: login/register email sanitizing must only trim + lowercase.
// Fails if normalizeEmail() ever comes back (it strips gmail dots/+suffixes,
// which breaks login for accounts whose stored email keeps the dots).
// Run: node scripts/check-email-validation.mjs
import assert from "node:assert/strict";
import { validateLogin, validateRegister } from "../middleware/validation.js";

const run = async (chain, body) => {
    const req = { body: { ...body } };
    for (const mw of chain.slice(0, -1)) await mw.run(req);
    return req.body.email;
};

const cases = [
    ["  Anu.PTE01@Gmail.com  ", "anu.pte01@gmail.com"],
    ["user+tag@gmail.com", "user+tag@gmail.com"],
    ["citiz970@GMAIL.com", "citiz970@gmail.com"],
];

for (const [input, expected] of cases) {
    assert.equal(await run(validateLogin, { email: input, password: "secret1" }), expected);
    assert.equal(
        await run(validateRegister, {
            email: input, full_name: "Anu B", phone: "9845805077",
            password: "secret1", role: "customer",
        }),
        expected
    );
}

console.log("OK — email sanitizing preserves dots and +suffixes");
