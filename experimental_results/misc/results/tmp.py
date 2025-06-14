import json
import subprocess

src = "Mono2Multi_Full"
dst = "Mono2Multi"
projects = ["JPetStore", "DayTrader", "AcmeAir", "Plants"]

for project in projects:
	with open(f"{src}/{src}_{project}.json", "rt") as file:
		data = json.load(file)
	new_data = []
	for d in data:
		if (0.05 <= d["threshold"] <= 0.1) and (0.8 <= d["alpha"] <= 0.95):
			new_data.append(d)
	with open(f"{dst}/tmp_{dst}_{project}.json", "wt") as output_file:
		json.dump(new_data, output_file, indent=2)
	in_ms = False
	inn_ms = False
	with (open(f"{dst}/tmp_{dst}_{project}.json", "rt") as in_f,
		open(f"{dst}/{dst}_{project}.json", "wt") as out_f):
		for line in in_f.readlines():
			if line.endswith("[\n"):
				if in_ms:
					inn_ms = True
				elif "microservices" in line:
					in_ms = True
					out_f.write(line[:-1])
					continue
			elif line.endswith("],\n") or line.endswith("]\n"):
				if inn_ms:
					inn_ms = False
				else:
					in_ms = False
					out_f.write(line.lstrip())
					continue
			if in_ms:
				out_f.write(line.strip())
			else:
				out_f.write(line)
	subprocess.run(["rm", f"{dst}/tmp_{dst}_{project}.json"])
