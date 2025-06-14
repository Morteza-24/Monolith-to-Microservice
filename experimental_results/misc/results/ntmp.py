import json
import subprocess
from copy import deepcopy


src = "Mo2oM"
dst = "Mo2oM_MinSamples"
projects = ["JPetStore", "DayTrader", "AcmeAir", "Plants"]

for project in projects:
	print(project)
	with open(f"{src}/{src}_{project}.json", "rt") as file:
		data = json.load(file)
	new_data = []
	for d in data:
		d["min_samples"] = 1
		new_data.append(d)
		for min_samples in range(2, 6):
			d_copy = deepcopy(d)
			ms_counts = [ms for ms_list in d_copy["microservices"] for ms in ms_list]
			for ms in set(ms_counts):
				if (ms != -1) and (ms_counts.count(ms) < min_samples):
					for i, ms_list in enumerate(d_copy["microservices"]):
						if ms in ms_list:
							d_copy["microservices"][i].remove(ms)
							if len(d_copy["microservices"][i]) == 0:
								d_copy["microservices"][i] = [-1]
			d_copy["min_samples"] = min_samples
			new_data.append(d_copy)
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
