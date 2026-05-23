#!/usr/bin/env python3
"""Rename bulk.* cases to meaningful category-based IDs.

Drops the opaque `bulk.<subcategory>.<NNN>` format in favour of the same
schema used by the rest of the suite: `<category>.<feature>.<behavior>`.

Also updates docs/junit provenance to match the new category, and adds
`behavior: pipeline` where the new category is not pipe-exempt and the
query has more than 3 pipe steps.
"""

from __future__ import annotations
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASES = ROOT / "gortools/src/test/resources/compat/cases/compatibility-cases.yml"

# ─────────────────────────────────────────────────────────────
# Rename mapping: old id → new id
# ─────────────────────────────────────────────────────────────
RENAMES: dict[str, str] = {
    # bulk.where.* → cmd.where.*
    "bulk.where.001": "cmd.where.norrows_rownum_eq_0",
    "bulk.where.002": "cmd.where.norrows_rownum_eq_1",
    "bulk.where.003": "cmd.where.norrows_rownum_eq_2",
    "bulk.where.004": "cmd.where.norrows_rownum_eq_3",
    "bulk.where.005": "cmd.where.norrows_rownum_eq_4",
    "bulk.where.006": "cmd.where.norrows_rownum_gt_0",
    "bulk.where.007": "cmd.where.norrows_rownum_gt_1",
    "bulk.where.008": "cmd.where.norrows_rownum_gt_2",
    "bulk.where.009": "cmd.where.norrows_rownum_gt_3",
    "bulk.where.010": "cmd.where.norrows_rownum_lt_1",
    "bulk.where.011": "cmd.where.norrows_rownum_lt_2",
    "bulk.where.012": "cmd.where.norrows_rownum_lt_3",
    "bulk.where.013": "cmd.where.norrows_rownum_lt_4",
    "bulk.where.014": "cmd.where.norrows_rownum_lte_0",
    "bulk.where.015": "cmd.where.norrows_rownum_lte_1",
    "bulk.where.016": "cmd.where.norrows_rownum_lte_2",
    "bulk.where.017": "cmd.where.norrows_rownum_gte_2",
    "bulk.where.018": "cmd.where.norrows_rownum_gte_3",
    "bulk.where.019": "cmd.where.norrows_rownum_gte_4",
    "bulk.where.020": "cmd.where.norrows_rownum_gte_0",
    # bulk.calc.* → cmd.calc.* / fn.math.* / fn.conv.*
    "bulk.calc.021": "cmd.calc.add_1_plus_1",
    "bulk.calc.022": "cmd.calc.add_2_plus_2",
    "bulk.calc.023": "cmd.calc.add_3_plus_3",
    "bulk.calc.024": "cmd.calc.add_4_plus_4",
    "bulk.calc.025": "cmd.calc.add_5_plus_5",
    "bulk.calc.026": "cmd.calc.sub_6_minus_1",
    "bulk.calc.027": "cmd.calc.sub_7_minus_2",
    "bulk.calc.028": "cmd.calc.sub_8_minus_3",
    "bulk.calc.029": "cmd.calc.mul_2_times_3",
    "bulk.calc.030": "cmd.calc.mul_3_times_3",
    "bulk.calc.031": "cmd.calc.mul_4_times_3",
    "bulk.calc.032": "cmd.calc.div_8_by_2",
    "bulk.calc.033": "cmd.calc.div_9_by_3",
    "bulk.calc.034": "cmd.calc.div_10_by_2",
    "bulk.calc.035": "cmd.calc.paren_mul_over_add",
    "bulk.calc.036": "cmd.calc.mul_before_add_no_paren",
    "bulk.calc.037": "cmd.calc.pow_2_cubed",
    "bulk.calc.038": "cmd.calc.pow_3_squared",
    "bulk.calc.039": "fn.math.abs_negative_int",
    "bulk.calc.040": "fn.math.sqrt_perfect_square",
    "bulk.calc.041": "fn.conv.int_from_float",
    "bulk.calc.042": "fn.conv.float_from_string",
    "bulk.calc.043": "fn.conv.number_from_expr",
    "bulk.calc.044": "fn.math.max_two_ints",
    "bulk.calc.045": "fn.math.min_two_ints",
    "bulk.calc.046": "fn.math.round_down",
    "bulk.calc.047": "fn.math.round_up",
    "bulk.calc.048": "fn.math.ceil_fraction",
    "bulk.calc.049": "fn.math.floor_fraction",
    "bulk.calc.050": "fn.math.mod_remainder",
    # bulk.string.* → fn.string.*
    "bulk.string.051": "fn.string.upper_lowercase_input",
    "bulk.string.052": "fn.string.lower_uppercase_input",
    "bulk.string.053": "fn.string.len_three_chars",
    "bulk.string.054": "fn.string.reverse_abc",
    "bulk.string.055": "fn.string.trim_spaces",
    "bulk.string.056": "fn.string.left_two_chars",
    "bulk.string.057": "fn.string.right_two_chars",
    "bulk.string.058": "fn.string.substr_slice",
    "bulk.string.059": "fn.string.mid_slice",
    "bulk.string.060": "fn.string.replace_char_in_string",
    "bulk.string.061": "fn.string.posof_char",
    "bulk.string.062": "fn.string.brackets_wrap",
    "bulk.string.063": "fn.string.unbracket",
    "bulk.string.064": "fn.string.str_from_int",
    "bulk.string.065": "fn.string.string_from_float",
    # bulk.list.* → fn.list.*
    "bulk.list.066": "fn.list.listfirst_three_elems",
    "bulk.list.067": "fn.list.listsecond_three_elems",
    "bulk.list.068": "fn.list.listlast_three_elems",
    "bulk.list.069": "fn.list.listsize_three_elems",
    "bulk.list.070": "fn.list.listtail_three_elems",
    "bulk.list.071": "fn.list.listnth_second_elem",
    "bulk.list.072": "fn.list.listindex_elem",
    "bulk.list.073": "fn.list.listadd_elem",
    "bulk.list.074": "fn.list.listreverse_abc",
    "bulk.list.075": "fn.list.listsortasc_strings",
    "bulk.list.076": "fn.list.listsortdesc_strings",
    "bulk.list.077": "fn.list.listnumsortasc",
    "bulk.list.078": "fn.list.listnumsortdesc",
    "bulk.list.079": "fn.list.listnumsum",
    "bulk.list.080": "fn.list.listnumavg",
    # bulk.pipe.* → appropriate cmd.*
    "bulk.pipe.081": "cmd.where.nor_calc_filter_gt_2",
    "bulk.pipe.082": "cmd.top.nor_calc_top_first",
    "bulk.pipe.083": "cmd.skip.nor_calc_skip_one",
    "bulk.pipe.084": "cmd.until.nor_calc_until_gte_3",
    "bulk.pipe.085": "cmd.distinct.nor_calc_all",
    "bulk.pipe.086": "cmd.hide.nor_calc_hide_rownum",
    "bulk.pipe.087": "cmd.tryhide.nor_calc_missing_col",
    "bulk.pipe.088": "cmd.tryselect.nor_calc_with_missing",
    "bulk.pipe.089": "cmd.select.nor_calc_two_cols_b",
    "bulk.pipe.090": "cmd.rename.nor_calc_rename_col",
    "bulk.pipe.091": "cmd.prefix.nor_calc_prefix_col",
    "bulk.pipe.092": "cmd.top.nor_calc_zero",
    "bulk.pipe.093": "cmd.where.nor_calc_filter_eq_2",
    "bulk.pipe.094": "cmd.where.nor_calc_filter_eq_3",
    "bulk.pipe.095": "cmd.where.nor_calc_filter_lte_2",
    "bulk.pipe.096": "cmd.where.nor_calc_filter_gte_2",
    # bulk.error.* → syntax.error.*
    "bulk.error.097": "syntax.error.norrows_where_no_condition",
    "bulk.error.098": "syntax.error.norrows_calc_no_name",
    "bulk.error.099": "syntax.error.sort_invalid_flag",
    "bulk.error.100": "syntax.error.group_invalid_flag",
    "bulk.error.101": "syntax.error.norrows_top_string_arg",
    "bulk.error.102": "syntax.error.skip_string_arg",
    "bulk.error.103": "syntax.error.upto_chr_arg",
    "bulk.error.104": "syntax.error.until_no_condition",
    "bulk.error.105": "syntax.error.norrows_select_no_args",
    "bulk.error.106": "syntax.error.norrows_hide_no_args",
    # bulk.cmd.* → appropriate cmd.* / fn.string.*
    "bulk.cmd.107": "cmd.select.nor_single_rownum",
    "bulk.cmd.108": "cmd.select.nor_chrom_and_rownum",
    "bulk.cmd.109": "cmd.select.nor_pos_and_rownum",
    "bulk.cmd.110": "cmd.hide.nor_rownum",
    "bulk.cmd.111": "cmd.hide.nor_posnor",
    "bulk.cmd.112": "cmd.hide.nor_chromnor",
    "bulk.cmd.113": "cmd.top.nor_select_then_top1",
    "bulk.cmd.114": "cmd.skip.nor_select_then_skip1",
    "bulk.cmd.115": "cmd.calc.nor_rownum_plus_1",
    "bulk.cmd.116": "cmd.select.nor_calc_top1",
    "bulk.cmd.117": "cmd.hide.nor_calc_rownum",
    "bulk.cmd.118": "cmd.select.nor_calc_two_cols",
    "bulk.cmd.119": "cmd.select.nor_string_calc_only",
    "bulk.cmd.120": "cmd.select.nor_rownum_and_string",
    "bulk.cmd.121": "cmd.hide.nor_string_calc_rownum",
    "bulk.cmd.122": "cmd.select.nor_string_calc_top1",
    "bulk.cmd.123": "cmd.skip.nor_rownum_skip4",
    "bulk.cmd.124": "cmd.top.nor_rownum_top3",
    "bulk.cmd.125": "cmd.top.nor_rownum_top_all",
    "bulk.cmd.126": "cmd.top.nor_rownum_top1",
    "bulk.cmd.127": "cmd.skip.nor_rownum_skip5",
    "bulk.cmd.128": "cmd.skip.nor_skip2_then_top1",
    "bulk.cmd.129": "cmd.skip.nor_skip3_then_top1",
    "bulk.cmd.130": "cmd.skip.nor_skip4_then_top1",
    "bulk.cmd.131": "cmd.top.nor_rownum_top2",
    "bulk.cmd.132": "cmd.top.nor_rownum_top4",
    "bulk.cmd.133": "cmd.calc.nor_offset_select_top1",
    "bulk.cmd.134": "cmd.calc.nor_offset_select_skip5",
    "bulk.cmd.135": "cmd.calc.nor_offset_select_all",
    "bulk.cmd.136": "cmd.select.nor_string_const_only",
    "bulk.cmd.137": "cmd.hide.nor_string_hide_pos",
    "bulk.cmd.138": "cmd.hide.nor_string_hide_chrom",
    "bulk.cmd.139": "cmd.select.nor_string_rownum_top1",
    "bulk.cmd.140": "cmd.select.nor_string_rownum_skip3",
    "bulk.cmd.141": "cmd.select.nor_string_only_top2",
    "bulk.cmd.142": "cmd.where.nor_calc_rownum_eq_2",
    "bulk.cmd.143": "cmd.where.nor_calc_rownum_eq_3",
    "bulk.cmd.144": "cmd.where.nor_calc_gte_top1_a",
    "bulk.cmd.145": "cmd.where.nor_calc_lte_top1",
    "bulk.cmd.146": "cmd.where.nor_calc_gt_2",
    "bulk.cmd.147": "cmd.where.nor_calc_lt_1",
    "bulk.cmd.148": "cmd.where.nor_calc_neq_top1",
    "bulk.cmd.149": "cmd.where.nor_calc_gte_4",
    "bulk.cmd.150": "cmd.where.nor_calc_lte_1",
    "bulk.cmd.151": "cmd.where.nor_calc_eq_2_b",
    "bulk.cmd.152": "cmd.where.nor_calc_eq_3_b",
    "bulk.cmd.153": "cmd.where.nor_calc_gt_top1",
    "bulk.cmd.154": "cmd.where.nor_calc_lt_top1",
    "bulk.cmd.155": "cmd.where.nor_calc_neq_top1_b",
    "bulk.cmd.156": "cmd.calc.nor_string_concat_a",
    "bulk.cmd.157": "cmd.calc.nor_string_concat_b",
    "bulk.cmd.158": "cmd.calc.nor_string_concat_c",
    "bulk.cmd.159": "cmd.calc.nor_string_concat_d",
    "bulk.cmd.160": "cmd.calc.nor_string_concat_e",
    "bulk.cmd.161": "cmd.sort.nor_calc_asc_top1",
    "bulk.cmd.162": "cmd.sort.nor_calc_desc_top1",
    "bulk.cmd.163": "cmd.sort.nor_calc_asc_top2",
    "bulk.cmd.164": "cmd.sort.nor_calc_asc_skip3",
    "bulk.cmd.166": "cmd.sort.nor_calc_desc_top2",
    "bulk.cmd.167": "cmd.sort.nor_calc_desc_skip3",
    "bulk.cmd.168": "cmd.where.nor_calc_gte_top1_b",
    "bulk.cmd.171": "cmd.skip.nor_calc_skip_then_top1",
    "bulk.cmd.172": "cmd.sort.nor_calc_five_asc_top1",
    "bulk.cmd.173": "cmd.sort.nor_calc_six_asc_skip_top1",
    "bulk.cmd.174": "cmd.where.nor_if_group_a_top1",
    "bulk.cmd.175": "cmd.where.nor_if_group_b_top1",
    "bulk.cmd.176": "cmd.sort.nor_if_sort_top1",
    "bulk.cmd.177": "cmd.sort.nor_if_sort_skip2_top1",
    "bulk.cmd.180": "cmd.where.nor_if_neq_a_top1",
    "bulk.cmd.181": "cmd.where.nor_if_neq_b_top1",
    "bulk.cmd.182": "cmd.calc.nor_chain_mul",
    "bulk.cmd.183": "cmd.calc.nor_chain_add_self",
    "bulk.cmd.184": "cmd.calc.nor_chain_sub",
    "bulk.cmd.185": "cmd.calc.nor_chain_div",
    "bulk.cmd.186": "cmd.calc.nor_chain_mod",
    "bulk.cmd.187": "cmd.where.nor_mod_odd_top1",
    "bulk.cmd.189": "cmd.where.nor_mod_even_skip1_top1",
    "bulk.cmd.191": "cmd.select.nor_mod_two_cols",
    "bulk.cmd.192": "cmd.hide.nor_mod_rownum",
    "bulk.cmd.193": "cmd.hide.nor_mod_posnor",
    "bulk.cmd.194": "cmd.hide.nor_mod_chromnor",
    "bulk.cmd.195": "cmd.select.nor_mod_top1",
    "bulk.cmd.196": "cmd.select.nor_mod_skip4",
    "bulk.cmd.197": "cmd.sort.nor_mod_sort_top1",
    "bulk.cmd.198": "cmd.sort.nor_mod_sort_skip2_top1",
    "bulk.cmd.201": "fn.string.nor_upper_in_calc",
    "bulk.cmd.202": "fn.string.nor_lower_in_calc",
    "bulk.cmd.203": "fn.string.nor_len_in_calc",
    "bulk.cmd.204": "fn.string.nor_substr_in_calc",
    "bulk.cmd.205": "fn.string.nor_replace_in_calc",
    "bulk.cmd.206": "fn.string.nor_concat_in_calc",
    # bulk.new.* → appropriate fn.* / cmd.*
    "bulk.new.001": "cmd.split.nor_split_then_relremove",
    "bulk.new.002": "cmd.deflatecolumn.data_string",
    "bulk.new.003": "cmd.deflatecolumn.inflate_roundtrip",
    "bulk.new.004": "cmd.distloc.merge_three_rows",
    "bulk.new.005": "cmd.segproj.merge_overlapping",
    "bulk.new.006": "fn.date.currentdate_year_len_4",
    "bulk.new.007": "fn.date.currentdate_dmy_len_10",
    "bulk.new.008": "fn.date.epoch_positive",
    "bulk.new.009": "fn.date.epoch_from_string_positive",
    "bulk.new.010": "fn.date.edate_year_from_epoch",
    "bulk.new.011": "fn.date.edate_from_current_epoch",
    "bulk.new.012": "fn.string.cscontains_case_match",
    "bulk.new.013": "fn.string.cscontains_case_no_match",
    "bulk.new.014": "fn.pr.beta_one_one_one",
    "bulk.new.015": "fn.pr.betac_one_one_one",
    "bulk.new.016": "fn.pr.binomial_one_one_one",
    "bulk.new.017": "fn.pr.binomialc_one_one_one",
    "bulk.new.018": "fn.pr.chi_zeros",
    "bulk.new.019": "fn.pr.chi2_range_check",
    "bulk.new.020": "fn.pr.chisquare_range_check",
    "bulk.new.021": "fn.pr.chisquarecompl_range_check",
    "bulk.new.022": "fn.pr.erf_zero",
    "bulk.new.023": "fn.pr.erfc_zero",
    "bulk.new.024": "fn.pr.gamma_range_check",
    "bulk.new.025": "fn.pr.gammac_range_check",
    "bulk.new.026": "fn.pr.invchisquare_roundtrip",
    "bulk.new.027": "fn.pr.invnormal_roundtrip",
    "bulk.new.028": "fn.pr.invstudent_half",
    "bulk.new.029": "fn.pr.negbinomial_one_one_one",
    "bulk.new.030": "fn.pr.negbinomialc_one_one_one",
    "bulk.new.031": "fn.pr.normal_half_range_check",
    "bulk.new.032": "fn.pr.poisson_zero_range_check",
    "bulk.new.033": "fn.pr.poissonc_zero_range_check",
    "bulk.new.034": "fn.pr.pval_zeros",
    "bulk.new.035": "fn.pr.pvalone_zeros",
    "bulk.new.036": "fn.pr.student_range_check",
    "bulk.new.037": "fn.genomic.iupacfa_empty",
    "bulk.new.038": "fn.genomic.iupacma_empty",
    "bulk.new.039": "fn.genomic.iupacgtstat_empty",
    "bulk.new.040": "fn.genomic.bamtag_empty_args",
    "bulk.new.041": "fn.genomic.gfftag_empty_args",
    "bulk.new.042": "fn.genomic.charsphased2gt_pair",
    "bulk.new.043": "fn.genomic.charsphased2gt_space",
    "bulk.new.044": "fn.genomic.iooa_test_string",
    "bulk.new.045": "fn.genomic.gtshare_all_empty",
    "bulk.new.046": "fn.math.random_in_0_1",
    "bulk.new.047": "fn.math.random_lt_1",
    "bulk.new.048": "fn.string.md5_hash",
    "bulk.new.049": "fn.string.md5_hello_world",
    "bulk.new.050": "fn.string.sleep_zero",
    "bulk.new.051": "fn.list.catch_oob_listnth",
    "bulk.new.052": "fn.diagnostic.arch_nonempty",
    "bulk.new.053": "fn.diagnostic.availcpu_nonneg",
    "bulk.new.054": "fn.diagnostic.cpuload_nonneg",
    "bulk.new.055": "fn.diagnostic.syscpuload_nonneg",
    "bulk.new.056": "fn.diagnostic.free_nonneg",
    "bulk.new.057": "fn.diagnostic.freemem_nonneg",
    "bulk.new.058": "fn.diagnostic.totalmem_nonneg",
    "bulk.new.059": "fn.diagnostic.maxmem_nonneg",
    "bulk.new.060": "fn.diagnostic.threadid_positive",
    "bulk.new.061": "fn.string.cscontains_lower_match",
    "bulk.new.062": "fn.string.cscontains_upper_no_match",
    "bulk.new.063": "fn.pr.beta_half_range_check",
    "bulk.new.064": "fn.pr.betac_half_range_check",
    "bulk.new.065": "fn.pr.chi2_alt_range_check",
    "bulk.new.066": "fn.pr.chisquare_small_range_check",
    "bulk.new.067": "fn.pr.erf_half_range_check",
    "bulk.new.068": "fn.pr.erfc_half_range_check",
    "bulk.new.069": "fn.pr.invnormal_84pct_range_check",
    "bulk.new.070": "fn.pr.invstudent_975_range_check",
    "bulk.new.071": "fn.pr.normal_one_range_check",
    "bulk.new.072": "fn.pr.poisson_one_range_check",
    "bulk.new.073": "fn.pr.poissonc_one_range_check",
    "bulk.new.074": "fn.pr.student_high_range_check",
    "bulk.new.075": "fn.genomic.bamtag_key_a",
    "bulk.new.076": "fn.genomic.gfftag_gene_key",
    "bulk.new.077": "fn.genomic.iooa_returns_one",
    "bulk.new.078": "fn.genomic.gtshare_returns_one",
    "bulk.new.079": "fn.string.md5_nonempty",
    "bulk.new.080": "fn.list.catch_oob_listnth_alt",
    "bulk.new.081": "fn.diagnostic.availcpu_if_nonneg",
    "bulk.new.082": "fn.diagnostic.cpuload_if_nonneg",
    "bulk.new.083": "fn.diagnostic.syscpuload_str_nonempty",
    "bulk.new.084": "fn.diagnostic.free_if_nonneg",
    "bulk.new.085": "fn.diagnostic.freemem_if_nonneg",
    "bulk.new.086": "fn.diagnostic.totalmem_if_nonneg",
    "bulk.new.087": "fn.diagnostic.maxmem_if_nonneg",
    "bulk.new.088": "fn.diagnostic.threadid_if_positive",
    "bulk.new.089": "fn.date.currentdate_iso_len_10",
    "bulk.new.090": "fn.date.edate_jan_2018",
    "bulk.new.091": "fn.date.epoch_dotted_date_positive",
    "bulk.new.094": "fn.genomic.iupacfa_returns_a",
    "bulk.new.095": "fn.genomic.iupacma_returns_c",
    "bulk.new.096": "fn.pr.binomial_eq_one",
    "bulk.new.097": "fn.pr.binomialc_eq_zero",
    "bulk.new.098": "fn.pr.negbinomial_eq_one",
    "bulk.new.099": "fn.pr.negbinomialc_eq_zero",
    "bulk.new.100": "fn.pr.pval_eq_one",
    "bulk.new.101": "fn.date.currentdate_iso_len_check",
    "bulk.new.102": "fn.date.epoch_from_string_ok",
    "bulk.new.103": "fn.date.edate_month_from_epoch",
    "bulk.new.104": "fn.date.edate_day_from_epoch",
    "bulk.new.105": "fn.date.edate_year_from_epoch_check",
    "bulk.new.106": "fn.date.currentdate_year_ok",
    "bulk.new.108": "fn.date.edate_iso_jan_2018",
    "bulk.new.109": "fn.date.epoch_positive_check",
    "bulk.new.110": "fn.date.edate_iso_len_10",
    "bulk.new.111": "fn.diagnostic.arch_positive_len",
    "bulk.new.114": "fn.diagnostic.syscpuload_nonneg_b",
    "bulk.new.121": "fn.string.md5_abc",
    "bulk.new.122": "fn.string.md5_len_positive",
    "bulk.new.124": "fn.list.catch_listnth_in_bounds",
    "bulk.new.125": "fn.list.catch_listnth_fallback",
    "bulk.new.126": "fn.string.cscontains_exact_case",
    "bulk.new.127": "fn.string.cscontains_wrong_case",
    "bulk.new.128": "fn.string.cscontains_substring_match",
    "bulk.new.129": "fn.string.cscontains_substring_no_match",
    "bulk.new.130": "fn.genomic.iooa_tttt_returns_one",
    "bulk.new.136": "fn.genomic.bamtag_key_b",
    "bulk.new.137": "fn.genomic.gfftag_id_key",
    "bulk.new.182": "fn.pr.pvalone_eq_one",
    "bulk.new.183": "fn.pr.chi_zeros_eq_zero",
    "bulk.new.184": "fn.pr.erf_zero_eq_zero",
    "bulk.new.185": "fn.pr.erfc_zero_eq_one",
    "bulk.new.200": "fn.date.currentdate_dmy_len_10_b",
}

# Cases that need `behavior: pipeline` added after rename
# (new category is not pipe-exempt but query has >3 pipe steps)
NEEDS_PIPELINE = {
    "cmd.sort.nor_calc_six_asc_skip_top1",    # was bulk.cmd.173 — 4 pipes
    "cmd.where.nor_mod_odd_top1",             # was bulk.cmd.187 — 4 pipes
    "cmd.where.nor_mod_even_skip1_top1",      # was bulk.cmd.189 — 5 pipes
    "cmd.select.nor_mod_top1",               # was bulk.cmd.195 — 4 pipes
    "cmd.select.nor_mod_skip4",              # was bulk.cmd.196 — 4 pipes
    "cmd.sort.nor_mod_sort_top1",            # was bulk.cmd.197 — 5 pipes
    "cmd.sort.nor_mod_sort_skip2_top1",      # was bulk.cmd.198 — 6 pipes
    "cmd.split.nor_split_then_relremove",    # was bulk.new.001 — 5 pipes
}

# ─────────────────────────────────────────────────────────────
# Provenance helper — mirrors backfill_compat_provenance logic
# ─────────────────────────────────────────────────────────────
DOC_CMD = "documentation/src/command"
DOC_FN = "documentation/src/functions"
JUNIT = "gortools/src/test/java/gorsat"

CMD_DOC = {
    "calc": "CALC.rst", "calcifmissing": "CALCIFMISSING.rst",
    "where": "WHERE.rst", "select": "SELECT.rst", "hide": "HIDE.rst",
    "top": "TOP.rst", "skip": "SKIP.rst", "sort": "SORT.rst",
    "rename": "RENAME.rst", "replace": "REPLACE.rst",
    "distinct": "DISTINCT.rst", "until": "UNTIL.rst", "upto": "UPTO.rst",
    "prefix": "PREFIX.rst", "tryhide": "TRYHIDE.rst",
    "tryselect": "TRYSELECT.rst", "split": "SPLIT.rst",
    "deflatecolumn": "DEFLATECOLUMN.rst", "distloc": "DISTLOC.rst",
    "segproj": "SEGSPAN.rst", "group": "GROUP.rst",
}
CMD_JUNIT = {
    "calc": f"{JUNIT}/Commands/UTestCalc.java",
    "where": f"{JUNIT}/UTestWhere.java",
    "select": f"{JUNIT}/UTestSelect.java",
    "hide": f"{JUNIT}/UTestHide.java",
    "top": f"{JUNIT}/UTestTop.java",
    "skip": f"{JUNIT}/UTestSkip.java",
    "sort": f"{JUNIT}/UTestSort.java",
    "rename": f"{JUNIT}/UTestRename.java",
    "replace": f"{JUNIT}/UTestReplace.java",
    "distinct": f"{JUNIT}/UTestDistinct.java",
    "prefix": f"{JUNIT}/Commands/UTestPrefix.java",
    "tryhide": f"{JUNIT}/UTestHide.java",
    "tryselect": f"{JUNIT}/UTestSelect.java",
    "split": f"{JUNIT}/UTestSplit.java",
    "deflatecolumn": f"{JUNIT}/UTestDeflateInflateColumn.java",
    "distloc": f"{JUNIT}",
    "segproj": f"{JUNIT}",
}
FN_DOC = {
    "math": "abs.rst", "alg": "abs.rst", "trig": "sine.rst",
    "string": "upper.rst", "list": "listadd.rst", "date": "date.rst",
    "bool": "if.rst", "conv": "int.rst", "convert": "int.rst",
    "genomic": "segoverlap.rst", "pr": "pval.rst",
    "diagnostic": "abs.rst",
}
FN_JUNIT = {
    "math": f"{JUNIT}/parser/UTestAlgebraicFunctions.java",
    "alg": f"{JUNIT}/parser/UTestAlgebraicFunctions.java",
    "trig": f"{JUNIT}/parser/UTestAlgebraicFunctions.java",
    "string": f"{JUNIT}/parser/UTestStringFunctions.java",
    "list": f"{JUNIT}/parser/UTestListFunctions.java",
    "date": f"{JUNIT}/parser/UTestDateFunctions.java",
    "bool": f"{JUNIT}/parser/UTestBooleanFunctions.java",
    "conv": f"{JUNIT}/parser/UTestCalcCompiler.java",
    "convert": f"{JUNIT}/parser/UTestCalcCompiler.java",
    "genomic": f"{JUNIT}/parser/UTestGenomicFunctions.java",
    "pr": f"{JUNIT}/parser/UTestPrFunctions.java",
    "diagnostic": f"{JUNIT}/parser/UTestDiagnosticFunctions.java",
}


def provenance_for(new_id: str) -> tuple[list[str], list[str]]:
    parts = new_id.split(".")
    head, sub = parts[0], parts[1] if len(parts) > 1 else ""
    if head == "cmd":
        doc_file = CMD_DOC.get(sub)
        docs = [f"{DOC_CMD}/{doc_file}"] if doc_file else ["documentation/src/commands.rst"]
        junit = [CMD_JUNIT.get(sub, JUNIT)]
        return docs, junit
    if head == "fn":
        doc_file = FN_DOC.get(sub)
        docs = [f"{DOC_FN}/{doc_file}"] if doc_file else ["documentation/src/all_functions.rst"]
        junit = [FN_JUNIT.get(sub, f"{JUNIT}/parser/UTestCalcCompiler.java")]
        return docs, junit
    if head == "syntax":
        return ["documentation/src/languageBasics.rst"], [f"{JUNIT}/UTestSyntaxChecker.java"]
    return ["documentation/src/languageBasics.rst"], [JUNIT]


# ─────────────────────────────────────────────────────────────
# Main processing
# ─────────────────────────────────────────────────────────────
ID_LINE = re.compile(r"^(- id:\s*)(\S+)(\s*)$")


def rewrite_block(block: list[str], new_id: str) -> list[str]:
    """Return the case block with id, docs, junit, and optionally behavior updated."""
    # Replace the id line
    result = []
    k = 0
    while k < len(block):
        line = block[k]
        if ID_LINE.match(line):
            result.append(f"- id: {new_id}")
            k += 1
            continue
        if line.strip().startswith("docs:"):
            # Skip old docs block (including indented items)
            k += 1
            while k < len(block) and (block[k].startswith("    ") or block[k].strip().startswith("- ")):
                k += 1
            # Write new docs
            docs, _ = provenance_for(new_id)
            result.append("  docs:")
            for d in docs:
                result.append(f'    - "{d}"')
            continue
        if line.strip().startswith("junit:"):
            # Skip old junit block
            k += 1
            while k < len(block) and (block[k].startswith("    ") or block[k].strip().startswith("- ")):
                k += 1
            # Write new junit
            _, junit = provenance_for(new_id)
            result.append("  junit:")
            for j in junit:
                result.append(f'    - "{j}"')
            continue
        if line.strip().startswith("behavior:"):
            # Remove any existing behavior line — we'll add it fresh below if needed
            k += 1
            continue
        result.append(line)
        k += 1

    if new_id in NEEDS_PIPELINE:
        # Insert behavior: pipeline after the id line
        for i, ln in enumerate(result):
            if ID_LINE.match(ln):
                result.insert(i + 1, "  behavior: pipeline")
                break

    return result


def main() -> int:
    text = CASES.read_text()
    lines = text.split("\n")
    out: list[str] = []
    renamed = 0
    skipped = 0
    i = 0
    while i < len(lines):
        line = lines[i]
        m = ID_LINE.match(line)
        if not m:
            out.append(line)
            i += 1
            continue
        old_id = m.group(2)
        if old_id not in RENAMES:
            out.append(line)
            i += 1
            skipped += 1
            continue
        new_id = RENAMES[old_id]
        # Collect the full case block
        j = i + 1
        while j < len(lines) and not lines[j].startswith("- id:"):
            j += 1
        block = lines[i:j]
        new_block = rewrite_block(block, new_id)
        out.extend(new_block)
        renamed += 1
        i = j

    CASES.write_text("\n".join(out))
    print(f"Renamed {renamed} case(s). Non-bulk cases left unchanged: {skipped}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
