def structural_similarity(ci, cj):

   if ci != 0 and cj != 0:

      return (1/2) * (calls(ci, cj) / callsin(cj) + calls(cj, ci) / callsin(ci))

   elif callsin(ci) == 0 and callsin(cj) != 0:

      return calls(ci, cj) / callsin(cj)

   elif callsin(ci) != 0 and callsin(cj) == 0:

      return calls(cj, ci) / callsin(ci)

   else:

      return 0