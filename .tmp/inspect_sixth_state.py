import sqlite3

db = r"c:\Users\user\AppData\Roaming\Code\User\globalStorage\state.vscdb"
con = sqlite3.connect(db)
cur = con.cursor()
print("DB:", db)
print("tables:", cur.execute("select name from sqlite_master where type='table'").fetchall())
rows = cur.execute("select key, value from ItemTable where lower(key) like '%sixth%' or lower(key) like '%auth%' or lower(key) like '%token%' or lower(key) like '%login%' order by key").fetchall()
print("matches:", len(rows))
for k, v in rows[:100]:
    print(k, repr(v[:200] if isinstance(v, str) else v))
