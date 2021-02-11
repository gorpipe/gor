/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.gor;

import gorsat.Commands.CommandParseUtilities;

public class ParseTiming {
    static final String MENDEL = "create ##dummy## = gor 1.mem | top 1;\n" +
            "\n" +
            "def ##ref## = /mnt/csa/env/dev/projects/clinical_examples/ref;\n" +
            "def ##source## = /mnt/csa/env/dev/projects/clinical_examples/source;\n" +
            "def #varcomments# = -Z dbscope=project_id#int#1 db://rda:rda.v_variant_annotations;\n" +
            "def #wesVars# = ##source##/var/wes_varcalls.gord -s PN;\n" +
            "def ##genes## = ##ref##/genes.gorz;\n" +
            "def ##exons## = ##ref##/ensgenes/ensgenes_codingexons.gorz;\n" +
            "def ##hgmd## = ##ref##/hgmd/hgmd_hgmd.gorz;\n" +
            "def ##gmap## = ##ref##/ensgenes/ensgenes.map;\n" +
            "def ##clinicalvars## = ##ref##/clinical_variants.gorz;\n" +
            "def ##clinicalvarsdetail## = ##ref##/clinical_variants_detail.gorz;\n" +
            "def ##gdismap## = ##ref##/ensgenes/ensgenes_disease.map;\n" +
            "def ##clinicalgenes## = ##ref##/clinical_genes.gorz;\n" +
            "def ##CGDmap## = ##ref##/disgenes/CGD.map;\n" +
            "def ##genepaneldiseases## = ##ref##/gene_panel_diseases.gorz;\n" +
            "def ##dbsnpclin## = ##ref##/dbsnp/dbsnp_clinvar.gorz;\n" +
            "def ##dbsnp## = ##ref##/dbsnp/dbsnp.gorz;\n" +
            "def ##repeat_regions## = ##ref##/simplerepeats.gorz;\n" +
            "def ##freqmax## = ##ref##/freq_max.gorz;\n" +
            "def ##ACMGset## = ##ref##/disgenes/ACMG_Minimum.set;\n" +
            "def ##CHILDRECESSset## = ##ref##/disgenes/childhood_recessive.set;\n" +
            "def ##CILIAset## = ##ref##/disgenes/cilia.set;\n" +
            "def ##defRmaxAf## = 0.03;\n" +
            "def ##defRmaxGf## = 0.0001;\n" +
            "def ##defDmaxAf## = 0.01;\n" +
            "def ##defMOI## = 'All';\n" +
            "def ##defVarCats## = 'Cat1,Cat1B,Cat2';\n" +
            "\n" +
            "create ##theFreqMax## = pgor ##freqmax##\n" +
            "| select 1-4,max_af;\n" +
            "\n" +
            "def ##VEP## = ##source##/anno/vep_v3-4-2/vep_single_wes.gord\n" +
            "| where 2=2\n" +
            "| varjoin -r -l -e 0 <(gor [##theFreqMax##]);\n" +
            "\n" +
            "create #CGDcandgenes# = gor ##genes##\n" +
            "| map ##gdismap## -c gene_symbol -h\n" +
            "| replace gene_in_disease replace(gene_in_disease,',','')\n" +
            "| map ##CGDmap## -c gene_symbol -h -m ''\n" +
            "| select 1-3,gene_symbol,gene_in_disease,MANIFESTATION_CATEGORIES,INTERVENTION_RATIONALE,COMMENTS,INTERVENTION_CATEGORIES,REFERENCES,CONDITION,INHERITANCE,AGE_GROUP\n" +
            "| rename Gene_in_disease GeneLists\n" +
            "| prefix 5#- CGD\n" +
            "| replace CGD_inheritance trim(CGD_inheritance)\n" +
            "| where CGD_MANIFESTATION_CATEGORIES != '' or CGD_INHERITANCE != ''\n" +
            "| select 1-3,gene_symbol;\n" +
            "\n" +
            "create #CGDtemp# = gor [#CGDcandgenes#]\n" +
            "| select 1,2,gene_symbol,CGD*\n" +
            "| distinct\n" +
            "| map ##gmap## -c gene_symbol -n gene_paralogs\n" +
            "| split gene_paralogs\n" +
            "| rename gene_paralogs gene_paralog\n" +
            "| where len(gene_paralog) > 1\n" +
            "| rename gene_symbol orig_gene\n" +
            "| calc tempchrom '1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,X,Y,M'\n" +
            "| split tempchrom\n" +
            "| calc newchrom 'chr'+tempchrom\n" +
            "| select newchrom,2,gene_paralog,orig_gene\n" +
            "| rename newchrom chrom\n" +
            "| sort genome\n" +
            "| group chrom -gc gene_paralog,orig_gene;\n" +
            "\n" +
            "create #CGDclingenes# = gor [#CGDtemp#]\n" +
            "| join -segseg -xl gene_paralog -xr gene_symbol ##genes##\n" +
            "| select 1,gene_start-,orig_gene,CGD*\n" +
            "| sort chrom\n" +
            "| where gene_symbol != orig_gene\n" +
            "| join -n -snpsnp -xl gene_symbol -xr gene_symbol [#CGDcandgenes#]\n" +
            "| group 1 -gc 3-gene_symbol -len 10000 -set -sc orig_gene\n" +
            "| rename set_orig_gene GENE_candidate_paralogs\n" +
            "| rename set_(.*) #{1}\n" +
            "| calc GENE_CandOrParalog 'p'\n" +
            "| merge <(gor [#CGDcandgenes#] | calc GENE_CandOrParalog 'c');\n" +
            "\n" +
            "create ##pardiseases## = gor ##ClinicalGenes##\n" +
            "| select 1-gene_diseases\n" +
            "| map -c gene_symbol ##gmap## -m '' -n gene_paralogs\n" +
            "| split gene_paralogs\n" +
            "| map -c gene_paralogs -m '' <(nor ##ClinicalGenes## | select gene_symbol,gene_diseases) -h\n" +
            "| rename gene_diseasesx par_diseases\n" +
            "| group 1 -gc gene_end-gene_diseases -set -sc gene_paralogs,par_diseases -len 10000\n" +
            "| replace set_par_diseases listdist(listfilter(set_par_diseases,'not(containsany(gene_diseases,x))'))\n" +
            "| rename set_par_diseases Gene_par_diseases\n" +
            "| rename set_gene_paralogs Gene_paralogs\n" +
            "| replace Gene_diseases,gene_par_diseases replace(#rc,',',', ')\n" +
            "| replace Gene_paralogs listfilter(Gene_paralogs,'x!=\".\"');\n" +
            "\n" +
            "create ##coveragevars## = pgor ##clinicalvars##\n" +
            "| select 1-4\n" +
            "| rename #2 Pos\n" +
            "| rename #3 Reference\n" +
            "| rename #4 Call\n" +
            "| merge <(gor #wesVars# -f 'BVL_INDEX' | select 1-4)\n" +
            "| varmerge reference call\n" +
            "| distinct;\n" +
            "\n" +
            "create ##indexcov## = pgor [##coveragevars##]\n" +
            "| select 1-4\n" +
            "| distinct\n" +
            "| join -snpseg -r -l -e 0 -maxseg 10000 <(gor ##source##/cov/segment_cov.gord -s PN -f 'BVL_INDEX')\n" +
            "| rename Depth apprCovDepth\n" +
            "| select 1,Pos,Reference,Call,apprCovDepth;\n" +
            "\n" +
            "create ##VEPKNOWN## = pgor <( ##VEP## | join -varseg -f 10 -i ##exons## | calc Gene_RmaxAf ##defRmaxAf## | calc Gene_RmaxGf ##defRmaxGf## | calc Gene_DmaxAf ##defDmaxAf## | calc Gene_MOI ##defMOI## | where isfloat(max_Af) and float(max_Af) <= Gene_RmaxAf | select 1-4,Max_Impact,Max_Consequence,Max_Af,Max_Score,Biotype,Refgene,Amino_Acids,CDS_position,Gene_Symbol,Protein_Position,Transcript_count,Gene_RmaxAf,Gene_RmaxGf,Gene_DmaxAf,Gene_MOI | join -varseg -n -f 2 ##repeat_regions## | varjoin -l -r -rprefix KNOWN <(gor ##clinicalvars## | select 1-Alt,Disease,MaxClinImpact,DbSource | rename Disease var_diseases | where MaxClinImpact = 'Pathogenic' | calc distance 0 | calc exactMatch 1 | group 1 -gc 3,4,distance,exactMatch -set -sc MaxClinImpact,var_diseases ) | rename KNOWN_set_var_diseases KNOWN_var_diseases | rename GENE_SYMBOL GENE | replace KNOWN_distance if(KNOWN_distance = '',1000,KNOWN_distance) | replace KNOWN_exactMatch if(KNOWN_exactMatch = '','0',KNOWN_exactMatch) | merge <( gor ##VEP## | join -varseg -f 10 -i ##exons## | calc Gene_RmaxAf ##defRmaxAf## | calc Gene_RmaxGf ##defRmaxGf## | calc Gene_DmaxAf ##defDmaxAf## | calc Gene_MOI ##defMOI## | where isfloat(max_Af) and float(max_Af) <= Gene_RmaxAf | select 1-4,Max_Impact,Max_Consequence,Max_Af,Max_Score,Biotype,Refgene,Amino_Acids,CDS_position,Gene_Symbol,Protein_Position,Transcript_count,Gene_RmaxAf,Gene_RmaxGf,Gene_DmaxAf,Gene_MOI | join -varseg -n -f 2 ##repeat_regions## | join -snpsnp -f 2 -rprefix KNOWN <(gor ##clinicalvars## | select 1-Alt,Disease,MaxClinImpact,DbSource | rename Disease var_diseases | where MaxClinImpact = 'Pathogenic' | group 1 -set -sc MaxClinImpact,var_diseases) | calc KNOWN_exactMatch 0 | rename distance KNOWN_distance | hide KNOWN_POS | rename KNOWN_set_var_diseases KNOWN_var_diseases | rename GENE_SYMBOL GENE ) | granno 1 -gc reference,call,gene -max -ic KNOWN_exactMatch | where KNOWN_exactMatch = max_KNOWN_exactMatch | calc absdist abs(KNOWN_distance) | granno 1 -gc reference,call,gene -min -ic absdist | where absdist = min_absdist | hide max_KNOWN_exactMatch,min_absdist,absdist | split KNOWN_var_diseases | calc KNOWN_distance_copy float(KNOWN_distance) | rename KNOWN_distance distance | group 1 -gc 3-distance -set -sc KNOWN* | rename set_(.*) #{1} ) | replace distance KNOWN_distance_copy | rename distance KNOWN_distance | replace KNOWN_distance if(KNOWN_distance in ('1000','1000.0'),'',KNOWN_distance) | rename max_af Max_Af | replace KNOWN_var_diseases if(KNOWN_var_diseases = '' and KNOWN_set_MaxClinImpact != '','unspecified',KNOWN_var_diseases); create ##CLINVARHGMD## = gor ##clinicalvarsdetail## | select 1-4,variantType,source | rename variantType KNOWN_variantType | rename source KNOWN_source | varjoin -r -l <(gor ##hgmd## | select 1,2,ref,allele,hgmdacc | rename hgmdacc KNOWN_HGMDacc) | varjoin -r -l <(gor ##dbsnpclin## | select 1,2,ref,alt,clnacc | rename CLNACC KNOWN_ClinVarAcc) | group 1 -gc ref,alt -set -sc known_* | rename set_(.*) #{1};\n" +
            "\n" +
            "def ##allvars## = gor #wesVars# -f 'BVL_INDEX'\n" +
            "| select 1-4\n" +
            "| distinct\n" +
            "| varjoin -r [##VEPKNOWN##]\n" +
            "| calc Cat4 IF(max_Af > 0.03,1,0)\n" +
            "| calc Cat1 IF(Cat4 = 0 and KNOWN_set_MaxClinImpact != '' and KNOWN_exactMatch = 1,1,0)\n" +
            "| calc Cat1B IF(Cat4 = 0 and Cat1 = 0 and KNOWN_set_MaxClinImpact != '' and KNOWN_exactMatch = 0,1,0)\n" +
            "| calc Cat2 IF(Cat4 = 0 and Cat1 = 0 and Cat1B = 0 and max_impact = 'HIGH',1,0)\n" +
            "| calc Cat3A IF(Cat4 = 0 and Cat1 = 0 and Cat1B = 0 and Cat2=0 and (max_Consequence = 'missense_variant' and max_score >= 0.9),1,0)\n" +
            "| calc Cat3B IF(Cat4 = 0 and Cat1 = 0 and Cat1B = 0 and Cat2=0 and Cat3A=0 and (max_impact = 'LOW' or (max_impact = 'MODERATE' and Cat3A = 0)),1,0)\n" +
            "| calc Cat IF(Cat1>0,'Cat1',IF(Cat1B>0,'Cat1B',IF(Cat2>0,'Cat2',IF(Cat3A>0,'Cat3A',IF(Cat3B>0,'Cat3B','Cat4')))))\n" +
            "| replace Cat4 IF(Cat = 'Cat4',1,0)\n" +
            "| hide Cat4,Cat1,Cat1B,Cat2,Cat3a,Cat3b,Known_set_MaxClinImpact\n" +
            "| rename Cat DIAG_ACMGcat;\n" +
            "\n" +
            "create ##indexvars## = pgor <(##allvars##)\n" +
            "| varjoin -r -l -e 'NA' <(gor #wesVars# -f 'BVL_INDEX' | where Depth = -1 or GL_Call >= 5 and Depth >= 8 and (CallCopies = 2 and CallRatio >= 0.66 or CallCopies = 1 and CallRatio >= 0.2 and CallRatio <= 1.0-0.2) | calc hetORhom IF(callCopies = 2,'hom','het') )\n" +
            "| calc Carrier IF(hetORhom = 'NA','false','true')\n" +
            "| where carrier != 'false';\n" +
            "\n" +
            "create ##ccgene## = pgor #wesVars# -f 'BVL_INDEX'\n" +
            "| varjoin -r <([##VEPKNOWN##] | select 1-4,MAX_IMPACT,MAX_CONSEQUENCE,max_Af,max_score,GENE,KNOWN_distance,KNOWN_set_MaxClinImpact,KNOWN_exactMatch,Gene_RmaxAf,Gene_RmaxGf,Gene_DmaxAf,Gene_MOI )\n" +
            "| calc hetORhom IF(callCopies = 2,'hom','het')\n" +
            "| calc hom IF(hetORhom = 'hom',1,0)\n" +
            "| calc het IF(hetORhom = 'het',1,0)\n" +
            "| rename max_score score\n" +
            "| calc known IF(KNOWN_set_MaxClinImpact != '',1,0)\n" +
            "| join -varseg -f 10 -xl GENE -xr GENE_SYMBOL <(gor ##genes## | select 1-4)\n" +
            "| group chrom -gc gene_Start,gene_End,PN,GENE -ic het,hom,known -fc score -sum\n" +
            "| select 1,4-\n" +
            "| sort chrom\n" +
            "| calc CHZ IF(sum_het>1 or sum_hom>0,1,0)\n" +
            "| calc WKV IF(sum_known>0,1,0)\n" +
            "| calc PNtype IF(PN='BVL_INDEX','GENE','UNKNOWN')\n" +
            "| calc VAR sum_het+sum_hom\n" +
            "| calc WVIG IF(VAR>0,1,0)\n" +
            "| group chrom -gc gene_Start,gene_End,PNtype,GENE -ic sum_hom,CHZ,WKV,VAR,WVIG,sum_known -sum\n" +
            "| rename sum_WVIG subjWithVarInGene\n" +
            "| select 1,4-\n" +
            "| rename sum_sum_hom homozVarsInGene\n" +
            "| rename sum_CHZ subjCompHeterInGene\n" +
            "| rename sum_WKV subjWithKnownVarInGene\n" +
            "| rename sum_Var varsInGene\n" +
            "| rename sum_sum_known knownVarsInGene\n" +
            "| sort chrom\n" +
            "| pivot -gc gene_End,GENE PNtype -v GENE -e 0;\n" +
            "\n" +
            "def ##autosomalreport## = gor [##indexvars##]\n" +
            "| where not (Chrom in ('chrX','chrM') )\n" +
            "| join -varseg -f 10 -l -r -e 0 -xl GENE -xr GENE [##ccgene##]\n" +
            "| replace GENE IF(GENE='0','OUTSIDE_GENES',GENE)\n" +
            "| calc DIAG_HomRecess IF( Carrier = 'true' and hetORhom = 'hom' , 'true', 'false')\n" +
            "| calc DIAG_CHZ IF( Carrier = 'true' and ( GENE_varsInGene > 1 ) , 'true', 'false')\n" +
            "| calc DIAG_Dominant IF( Carrier = 'true' and max_Af <= Gene_DmaxAf and 2=3 , 'true', 'false')\n" +
            "| prefix CallRatio,CallCopies,Depth,GL_Call,FILTER GT\n" +
            "| prefix Max_Impact-Max_Af,Max_Score-CDS_position,Protein_Position,Transcript_count VEP\n" +
            "| hide GENEx,GENE_subjWithKnownVarInGene,GENE_subjCompHeterInGene,GENE_subjWithVarInGene\n" +
            "| rename GENE GENE_symbol;\n" +
            "\n" +
            "create ##exvars## = pgor ##genes##\n" +
            "| select 1-3,gene_symbol\n" +
            "| join -segvar -f 10 -xl gene_symbol -xr gene <( gor #wesVars# -f 'BVL_INDEX' | where Depth = -1 or GL_Call >= 5 and Depth >= 8 and (CallCopies = 2 and CallRatio >= 0.66 or CallCopies = 1 and CallRatio >= 0.2 and CallRatio <= 1.0-0.2) | join -varseg -f 10 -i ##exons## | calc hetORhom IF(callCopies = 2,'hom','het') | varjoin -r <(gor [##VEPKNOWN##] | select 1-4,MAX_IMPACT,MAX_CONSEQUENCE,max_Af,max_score,GENE,KNOWN_distance,KNOWN_set_MaxClinImpact,KNOWN_exactMatch,Gene_RmaxAf,Gene_RmaxGf,Gene_DmaxAf,Gene_MOI) | calc Cat4 IF(max_Af > 0.03,1,0) | calc Cat1 IF(Cat4 = 0 and KNOWN_set_MaxClinImpact != '' and KNOWN_exactMatch = 1,1,0) | calc Cat1B IF(Cat4 = 0 and Cat1 = 0 and KNOWN_set_MaxClinImpact != '' and KNOWN_exactMatch = 0,1,0) | calc Cat2 IF(Cat4 = 0 and Cat1 = 0 and Cat1B = 0 and max_impact = 'HIGH',1,0) | calc Cat3A IF(Cat4 = 0 and Cat1 = 0 and Cat1B = 0 and Cat2=0 and (max_Consequence = 'missense_variant' and max_score >= 0.9),1,0) | calc Cat3B IF(Cat4 = 0 and Cat1 = 0 and Cat1B = 0 and Cat2=0 and Cat3A=0 and (max_impact = 'LOW' or (max_impact = 'MODERATE' and Cat3A = 0)),1,0) | calc Cat IF(Cat1>0,'Cat1',IF(Cat1B>0,'Cat1B',IF(Cat2>0,'Cat2',IF(Cat3A>0,'Cat3A',IF(Cat3B>0,'Cat3B','Cat4'))))) | replace Cat4 IF(Cat = 'Cat4',1,0) );\n" +
            "\n" +
            "create ##chz## = pgor [##exvars##]\n" +
            "| join -snpsnp [##exvars##] -xl PN,gene_symbol -xr PN,gene_symbol\n" +
            "| where pos = posx and (hetORhom = 'hom' or call != callx) or pos != posx\n" +
            "| where max_Af*max_Afx <= Gene_RmaxGf\n" +
            "| calc sum_Cat1 (Cat1+Cat1B)*IF(hetORhom='hom',2,1)+(Cat1x+Cat1Bx)*IF(pos != posx,1,0)\n" +
            "| calc sum_Cat2 Cat2*IF(hetORhom='hom',2,1)+Cat2x*IF(pos != posx,1,0)\n" +
            "| calc sum_Cat3a Cat3a*IF(hetORhom='hom',2,1)+Cat3ax*IF(pos != posx,1,0)\n" +
            "| calc sum_Cat3b Cat3b*IF(hetORhom='hom',2,1)+Cat3bx*IF(pos != posx,1,0)\n" +
            "| calc sum_Cat4 Cat4*IF(hetORhom='hom',2,1)+Cat4x*IF(pos != posx,1,0)\n" +
            "| calc Diagnosis IF(sum_Cat1+sum_Cat2 > 1,'1:DxConsistent', IF(sum_Cat1+sum_cat2 > 0 and sum_Cat3a > 0,'2:DxLikely', IF(sum_Cat3a > 1 or (sum_Cat1+sum_Cat2+sum_Cat3a > 0 and sum_Cat3b > 0),'3:DxPossible', IF(sum_Cat3b > 1,'4:DxLessLikely', IF(sum_Cat1+sum_Cat2 > 0 and sum_Cat4 > 0,'5:DxIndeterminate', IF(sum_Cat3a+sum_Cat3b > 0 and sum_Cat4 > 0,'6:DxUnlikely','7:DxNegative'))))))\n" +
            "| calc diag_order left(Diagnosis,1)\n" +
            "| hide Cat4-Cat3B,sum_Cat1-sum_Cat4,Cat4x-Cat3Bx\n" +
            "| select 1,pos,reference,call,hetorhom,PN,gene,posx,Cat,Catx,Diagnosis,Diag_order,referencex,callx\n" +
            "| sort 3000000\n" +
            "| rank 1 -gc reference,call,pn,gene diag_order -o asc\n" +
            "| where rank_diag_order = 1\n" +
            "| group 1 -gc reference,call,hetorhom,PN,gene,Cat,Diagnosis -set -sc Catx,posx\n" +
            "| rename Cat ACMGcat\n" +
            "| rename set_Catx otherACMGcat\n" +
            "| rename set_posx otherPos\n" +
            "| replace otherPos replace(replace(replace(otherPos,','+pos,''),pos+',',''),pos,'')\n" +
            "| replace otherACMGcat IF(otherPos='','',otherACMGcat)\n" +
            "| rename Diagnosis recessiveCat\n" +
            "| prefix ACMGcat-otherACMGcat DIAG\n" +
            "| hide hetORhom,PN\n" +
            "| rename GENE GENE_symbol;\n" +
            "\n" +
            "create ##final## = pgor <(##autosomalreport##)\n" +
            "| varjoin -l -r -xl gene_symbol -xr gene_symbol -rprefix DIAG -maxseg 3000000 <(gor [##exvars##] | select 1,pos,reference,call,gene_symbol | sort 3000000 | distinct)\n" +
            "| hide DIAG_GENE_SYMBOL\n" +
            "| varjoin -r -l -xl GENE_SYMBOL -xr GENE_SYMBOL [##chz##]\n" +
            "| hide GENE_SYMBOLx,DIAG_ACMGcatx\n" +
            "| replace DIAG_HomRecess IF(DIAG_recessiveCat = '','false',DIAG_HomRecess)\n" +
            "| replace DIAG_CHZ IF(DIAG_recessiveCat = '','false',DIAG_CHZ)\n" +
            "| calc DIAG_model = 'Autosomal'\n" +
            "| calc recessive_score IF(hetORhom = 'hom',2,0)+IF(DIAG_recessiveCat='1:DxConsistent',4,IF(DIAG_recessiveCat='2:DxLikely',2,IF(DIAG_recessiveCat='3:DxPossible',1,0)))+ 0.0\n" +
            "| calc dominant_score 0.0+ 0.0\n" +
            "| varjoin -l -r ##dbSNP##;\n" +
            "\n" +
            "def ###xlinkedstartshere### = xxxxxxxxxxxxxxxxxxxxxxxxxxxx;\n" +
            "\n" +
            "create ##Xindexvars## = gor -p chrX <(##allvars##)\n" +
            "| varjoin -r -l -e 'NA' <(gor #wesVars# -f 'BVL_INDEX' | where Depth = -1 or GL_Call >= 5 and Depth >= 8 and (CallCopies = 2 and CallRatio >= 0.66 or CallCopies = 1 and CallRatio >= 0.2 and CallRatio <= 1.0-0.2) | calc hetORhom IF(callCopies = 2,'hom','het') )\n" +
            "| calc Carrier IF(hetORhom = 'NA','false','true')\n" +
            "| where carrier = 'true';\n" +
            "\n" +
            "create ##Xccgene## = gor -p chrX #wesVars# -f 'BVL_INDEX'\n" +
            "| varjoin -r <([##VEPKNOWN##] | select 1-4,MAX_IMPACT,MAX_CONSEQUENCE,max_Af,max_score,GENE,KNOWN_distance,KNOWN_set_MaxClinImpact,KNOWN_exactMatch,Gene_RmaxAf,Gene_RmaxGf,Gene_DmaxAf,Gene_MOI )\n" +
            "| calc hetORhom IF(callCopies = 2,'hom','het')\n" +
            "| calc hom IF(hetORhom = 'hom',1,0)\n" +
            "| calc het IF(hetORhom = 'het',1,0)\n" +
            "| rename max_score score\n" +
            "| calc known IF(KNOWN_set_MaxClinImpact != '',1,0)\n" +
            "| join -varseg -f 10 -xl GENE -xr GENE_SYMBOL <(gor ##genes## | select 1-4)\n" +
            "| group chrom -gc gene_Start,gene_End,PN,GENE -ic het,hom,known -fc score -sum\n" +
            "| select 1,4-\n" +
            "| sort chrom\n" +
            "| calc CHZ IF(sum_het>1 or sum_hom>0,1,0)\n" +
            "| calc WKV IF(sum_known>0,1,0)\n" +
            "| calc PNtype IF(PN='BVL_INDEX','GENE','UNKNOWN')\n" +
            "| calc VAR sum_het+sum_hom\n" +
            "| calc WVIG IF(VAR>0,1,0)\n" +
            "| split PNtype\n" +
            "| group chrom -gc gene_Start,gene_End,PNtype,GENE -ic sum_hom,CHZ,WKV,VAR,WVIG,sum_known -sum\n" +
            "| rename sum_WVIG subjWithVarInGene\n" +
            "| select 1,4-\n" +
            "| rename sum_sum_hom homozVarsInGene\n" +
            "| rename sum_CHZ subjCompHeterInGene\n" +
            "| rename sum_WKV subjWithKnownVarInGene\n" +
            "| rename sum_Var varsInGene\n" +
            "| rename sum_sum_known knownVarsInGene\n" +
            "| sort chrom\n" +
            "| pivot -gc gene_End,GENE PNtype -v GENE -e 0;\n" +
            "\n" +
            "def ##Xlinkedreport## = gor -p chrX [##Xindexvars##]\n" +
            "| join -varseg -f 10 -l -r -e 0 -xl GENE -xr GENE [##Xccgene##]\n" +
            "| replace GENE IF(GENE='0','OUTSIDE_GENES',GENE)\n" +
            "| calc DIAG_HomRecess IF( Carrier = 'true' and 2=2 , 'true', 'false')\n" +
            "| calc DIAG_CHZ IF( Carrier = 'true' and (GENE_varsInGene > 0) , 'true', 'false')\n" +
            "| calc DIAG_Dominant IF( Carrier = 'true' and max_Af <= Gene_DmaxAf and 2=3 and ( Chrom = 'chrX' ) , 'true', 'false')\n" +
            "| prefix CallRatio,CallCopies,Depth,GL_Call,FILTER GT\n" +
            "| prefix Max_Impact-Max_Af,Max_Score-CDS_position,Protein_Position,Transcript_count VEP\n" +
            "| hide GENEx,GENE_subjWithKnownVarInGene,GENE_subjCompHeterInGene,GENE_subjWithVarInGene\n" +
            "| rename GENE GENE_symbol;\n" +
            "\n" +
            "create ##Xexvars## = gor -p chrX ##genes##\n" +
            "| select 1-3,gene_symbol\n" +
            "| join -segvar -f 10 -xl gene_symbol -xr gene <( gor #wesVars# -f 'BVL_INDEX' | where Depth = -1 or GL_Call >= 5 and Depth >= 8 and (CallCopies = 2 and CallRatio >= 0.66 or CallCopies = 1 and CallRatio >= 0.2 and CallRatio <= 1.0-0.2) | join -varseg -f 10 -i ##exons## | calc hetORhom IF(callCopies = 2,'hom','het') | varjoin -r <(gor [##VEPKNOWN##] | select 1-4,MAX_IMPACT,MAX_CONSEQUENCE,max_Af,max_score,GENE,KNOWN_distance,KNOWN_set_MaxClinImpact,KNOWN_exactMatch,Gene_RmaxAf,Gene_RmaxGf,Gene_DmaxAf,Gene_MOI) | calc Cat4 IF(max_Af > 0.03,1,0) | calc Cat1 IF(Cat4 = 0 and KNOWN_set_MaxClinImpact != '' and KNOWN_exactMatch = 1,1,0) | calc Cat1B IF(Cat4 = 0 and Cat1 = 0 and KNOWN_set_MaxClinImpact != '' and KNOWN_exactMatch = 0,1,0) | calc Cat2 IF(Cat4 = 0 and Cat1 = 0 and Cat1B = 0 and max_impact = 'HIGH',1,0) | calc Cat3A IF(Cat4 = 0 and Cat1 = 0 and Cat1B = 0 and Cat2=0 and (max_Consequence = 'missense_variant' and max_score >= 0.9),1,0) | calc Cat3B IF(Cat4 = 0 and Cat1 = 0 and Cat1B = 0 and Cat2=0 and Cat3A=0 and (max_impact = 'LOW' or (max_impact = 'MODERATE' and Cat3A = 0)),1,0) | calc Cat IF(Cat1>0,'Cat1',IF(Cat1B>0,'Cat1B',IF(Cat2>0,'Cat2',IF(Cat3A>0,'Cat3A',IF(Cat3B>0,'Cat3B','Cat4'))))) | replace Cat4 IF(Cat = 'Cat4',1,0) );\n" +
            "\n" +
            "create ##Xchz## = gor -p chrX [##Xexvars##]\n" +
            "| join -snpsnp [##Xexvars##] -xl PN,gene_symbol -xr PN,gene_symbol\n" +
            "| where max_Af*max_Afx <= Gene_RmaxGf\n" +
            "| calc sum_Cat1 (Cat1+Cat1B)*IF(hetORhom='hom',2,1)+(Cat1x+Cat1Bx)*IF(pos != posx,1,0)\n" +
            "| calc sum_Cat2 Cat2*IF(hetORhom='hom',2,1)+Cat2x*IF(pos != posx,1,0)\n" +
            "| calc sum_Cat3a Cat3a*IF(hetORhom='hom',2,1)+Cat3ax*IF(pos != posx,1,0)\n" +
            "| calc sum_Cat3b Cat3b*IF(hetORhom='hom',2,1)+Cat3bx*IF(pos != posx,1,0)\n" +
            "| calc sum_Cat4 Cat4*IF(hetORhom='hom',2,1)+Cat4x*IF(pos != posx,1,0)\n" +
            "| calc Diagnosis IF(sum_Cat1+sum_Cat2 > 1,'1:DxConsistent', IF(sum_Cat1+sum_cat2 > 0 and sum_Cat3a > 0,'2:DxLikely', IF(sum_Cat3a > 1 or (sum_Cat1+sum_Cat2+sum_Cat3a > 0 and sum_Cat3b > 0),'3:DxPossible', IF(sum_Cat3b > 1,'4:DxLessLikely', IF(sum_Cat1+sum_Cat2 > 0 and sum_Cat4 > 0,'5:DxIndeterminate', IF(sum_Cat3a+sum_Cat3b > 0 and sum_Cat4 > 0,'6:DxUnlikely','7:DxNegative'))))))\n" +
            "| calc diag_order left(Diagnosis,1)\n" +
            "| hide Cat4-Cat3B,sum_Cat1-sum_Cat4,Cat4x-Cat3Bx\n" +
            "| select 1,pos,reference,call,hetorhom,PN,gene,posx,Cat,Catx,Diagnosis,Diag_order,referencex,callx\n" +
            "| sort 3000000\n" +
            "| rank 1 -gc reference,call,pn,gene diag_order -o asc\n" +
            "| where rank_diag_order = 1\n" +
            "| group 1 -gc reference,call,hetorhom,PN,gene,Cat,Diagnosis -set -sc Catx,posx\n" +
            "| rename Cat ACMGcat\n" +
            "| rename set_Catx otherACMGcat\n" +
            "| rename set_posx otherPos\n" +
            "| replace otherPos replace(replace(replace(otherPos,','+pos,''),pos+',',''),pos,'')\n" +
            "| replace otherACMGcat IF(otherPos='','',otherACMGcat)\n" +
            "| rename Diagnosis recessiveCat\n" +
            "| prefix ACMGcat-otherACMGcat DIAG\n" +
            "| hide hetORhom,PN\n" +
            "| rename GENE GENE_symbol;\n" +
            "\n" +
            "create ##Xfinal## = gor -p chrX <(##Xlinkedreport##)\n" +
            "| varjoin -l -r -xl gene_symbol -xr gene_symbol -rprefix DIAG -maxseg 3000000 <(gor [##Xexvars##] | select 1,pos,reference,call,gene_symbol | sort 3000000 | distinct)\n" +
            "| hide DIAG_GENE_SYMBOL\n" +
            "| varjoin -r -l -xl GENE_SYMBOL -xr GENE_SYMBOL [##Xchz##]\n" +
            "| hide GENE_SYMBOLx,DIAG_ACMGcatx\n" +
            "| replace DIAG_HomRecess IF(DIAG_recessiveCat = '','false',DIAG_HomRecess)\n" +
            "| replace DIAG_CHZ IF(DIAG_recessiveCat = '','false',DIAG_CHZ)\n" +
            "| calc DIAG_model = 'Xlinked'\n" +
            "| calc recessive_score IF(hetORhom = 'hom',2,0)+IF(DIAG_recessiveCat='1:DxConsistent',4,IF(DIAG_recessiveCat='2:DxLikely',2,IF(DIAG_recessiveCat='3:DxPossible',1,0)))+ 0.0\n" +
            "| calc dominant_score 0.0+ 0.0\n" +
            "| varjoin -l -r ##dbSNP##;\n" +
            "\n" +
            "create ##uncommentedreport## = gor [##final##]\n" +
            "| merge [##Xfinal##]\n" +
            "| rank genome recessive_score -o desc\n" +
            "| rank genome dominant_score -o desc\n" +
            "| hide recessive_score,dominant_score\n" +
            "| rename rank_recessive_score DIAG_rank_recessive\n" +
            "| rename rank_dominant_score DIAG_rank_dominant\n" +
            "| join -varseg -l -f 10 -r -xl gene_symbol -xr gene_symbol <(gor ##genes## | map ##gmap## -c gene_symbol -m ? -n GENE_Aliases,OMIM_IDs,OMIM_Descriptions,GENE_Paralogs,GO_IDs,GO_Descriptions)\n" +
            "| hide gene_symbolx\n" +
            "| calc varType if(len(reference)=len(call),'sub',if(len(call)<len(reference) and substr(reference,0,len(call)) = call,'del',if(len(call)>len(reference) and substr(call,0,len(reference)) = reference,'ins','indel') ))\n" +
            "| calc Sequence_Variant chrom+':'+pos+', ' +if(Gene_Symbol != '',Gene_symbol,'') +if(not(VEP_CDS_Position in ('','.')),' c.' +if(contains(VEP_CDS_Position,','),'('+VEP_CDS_Position+')',VEP_CDS_Position)+'_',' ')+reference+'>'+call +if(not(VEP_Protein_Position in ('','.')),' p.' +if(contains(VEP_Protein_Position,','),'('+VEP_Protein_Position+')',VEP_Protein_Position)+'_'+replace(VEP_Amino_Acids,'/','>'),'') +', '+if(DIAG_ACMGcat != '',DIAG_ACMGcat+', ','')+if(hetORhom != 'NA',hetORhom+'_','') +varType +', '+VEP_Max_Consequence\n" +
            "| varjoin -l -r <(gor ##hgmd## | select 1-4,pmid | rename pmid Known_pmid )\n" +
            "| join -varseg -l -f 10 -r -rprefix GENE -xl gene_symbol -xr gene_symbol <(gor ##source##/cov/gene_cov_coding_seg.gord -s PN -f 'BVL_INDEX' | map -c gene_symbol ##gmap## -n gene_aliases -m 'missing' | split gene_aliases | replace gene_symbol if(gene_aliases != 'missing',gene_aliases,gene_symbol) | hide gene_aliases)\n" +
            "| join -varseg -l -f 10 -r -rprefix KNOWN -xl GENE_symbol -xr GENE_SYMBOL <(gor [##pardiseases##] | select 1-3,gene_symbol,gene_diseases,gene_par_diseases | replace gene_diseases if(listsize(#rc)>1,'\"'+replace(#rc,',','\", \"')+'\"',#rc) )\n" +
            "| hide GENE_gene_symbol,GENE_PN,KNOWN_GENE_SYMBOL\n" +
            "| join -varseg -l -r -xl gene_symbol -xr gene_symbol <(gor ##genes## | map ##gdismap## -c gene_symbol -h -m '' | map ##CGDmap## -c gene_symbol -h -m '' | select 1-3,gene_symbol,gene_in_disease,MANIFESTATION_CATEGORIES,INTERVENTION_RATIONALE,COMMENTS,INTERVENTION_CATEGORIES,REFERENCES,CONDITION,INHERITANCE,AGE_GROUP | rename Gene_in_disease KNOWN_GeneLists | prefix 6#- CGD | replace CGD_inheritance trim(CGD_inheritance) | replace KNOWN_GeneLists listfilter(KNOWN_GeneLists,'len(x)>0') | join -segseg -r -l -xl gene_symbol -xr gene_symbol <(gor ##genepaneldiseases## | split diseases | rename panel panels | group 1 -gc #3,gene_symbol -len 10000 -set -dis -sc panels,diseases | replace set* if(listsize(#rc)>1,'\"'+replace(#rc,',','\", \"')+'\"',#rc) | rename set_(.*) EuroGenetest_#{1} | rename dis_(.*) EuroGenetest_NoOf#{1} ) )\n" +
            "| hide gene_symbolx,gene_symbolxx\n" +
            "| calc KNOWN_InACMG IF(contains(KNOWN_GeneLists,'acmg'),'true','false')\n" +
            "| varjoin -r -l [##CLINVARHGMD##]\n" +
            "| columnsort 1,2,Reference,Call,hetORhom,PN,Gene_Symbol,Sequence_Variant,Carrier,DIAG*,VEP*;\n" +
            "\n" +
            "create ##commentfile## = gor #varcomments# #varcomments#\n" +
            "| where PN = 'BVL_INDEX'\n" +
            "| group 1 -gc 3,4 -len 500 -set -sc 5-\n" +
            "| rename set_(.*) #{1};\n" +
            "\n" +
            "create ##thefinalreport## = pgor [##uncommentedreport##]\n" +
            "| calc DIAG_deNovo 'false'\n" +
            "| varjoin -r -l -rprefix COMM <(gor [##commentfile##])\n" +
            "| replace DIAG_HomRecess,DIAG_CHZ if(chrom = 'chrY','false',#rc)\n" +
            "| calc Gene_cov if(isfloat(gene_lt5),'L:'+form(gene_lt5,4,2)+'M:'+form((gene_lt10-gene_lt5),4,2)+'H:'+form((1-gene_lt10),4,2),'NA')\n" +
            "| varjoin -l -r -e 'N/A' <(gor ##dbsnpclin## | select 1-5,CLNACC | distinct)\n" +
            "| columnsort 1,2,Reference,Call,hetORhom,PN,Sequence_Variant,Carrier,DIAG*,CGD*,VEP*,GENE*;\n" +
            "\n" +
            "gor [##thefinalreport##]\n" +
            "| select 1,2,KNOWN_Gene_par_diseases\n" +
            "| distinct\n" +
            "| sort genome -c *\n";

    static final String QC = "/* QC demo */\n" +
            "\n" +
            " def ##ref## = /mnt/csa/env/test/projects/clinical_examples/ref;\n" +
            "\n" +
            "def ##source## = /mnt/csa/env/test/projects/clinical_examples/source;\n" +
            "def #wesVars# = ##source##/var/wes_varcalls.gord -s PN;\n" +
            "def ##genes## = ##ref##/genes.gorz;\n" +
            "def ##exons## = ##ref##/ensgenes/ensgenes_codingexons.gorz;\n" +
            "def ##dbsnp## = ##ref##/dbsnp/dbsnp.gorz;\n" +
            "def ##freqmax## = ##ref##/freq_max.gorz;\n" +
            "\n" +
            "create #theFreqMax# = pgor ##freqmax##\n" +
            "| select 1-4,max_af;\n" +
            "\n" +
            "def ##VEP## = gor  ##source##/anno/vep_v3-4-2/vep_single_wes.gord\n" +
            "| select 1-4,max_impact,max_consequence\n" +
            "| varjoin -r -l -e 0 <(gor [#theFreqMax#]);\n" +
            "\n" +
            "create #chrgenecoverageindex# = pgor ##source##/cov/gene_cov_coding_seg.gord -s PN -f 'BVL_INDEX'\n" +
            "| calc candidate 0\n" +
            "| calc exlt5 exomesize*lt5\n" +
            "| calc exlt10 exomesize*lt10\n" +
            "| calc exlt15 exomesize*lt15\n" +
            "| calc exlt20 exomesize*lt20\n" +
            "| calc exlt25 exomesize*lt25\n" +
            "| calc exlt30 exomesize*lt30\n" +
            "| calc attribute '95% gene depth>20,90% gene depth>20,85% gene depth>20,95% gene depth>10,90% gene depth>10,85% gene depth>10'+',exlt5,exlt10,exlt15,exlt20,exlt25,exlt30'\n" +
            "| calc values ''+IF(lt20<0.05,'1','0')+','+IF(lt20<0.1,'1','0')+','+IF(lt20<0.15,'1','0')+','+IF(lt10<0.05,'1','0')+','+IF(lt10<0.1,'1','0')+','+IF(lt10<0.15,'1','0') +','+exlt5+','+exlt10+','+exlt15+','+exlt20+','+exlt25+','+exlt30\n" +
            "| calc bases avg_depth*exomesize\n" +
            "| split attribute,values\n" +
            "| group chrom -gc PN,attribute,candidate -fc values,bases,exomesize -sum -count;\n" +
            "\n" +
            "create #chrgenecoveragesubjects# = pgor ##source##/cov/gene_cov_coding_seg.gord -s PN -f 'CharcotMT_Father','CharcotMT_Mother','DENOVO_FATHER','DENOVO_MOTHER','OVAR2','OVAR4','OVAR6','OVAR8','OVAR11','OVAR13','AHCFATHER','AHCINDEX','AHCMOTHER','AHCBROTHER','BVL_FATHER','BVL_MOTHER','CAMYO_CASE','OVAR15','ATMCARR2','ATMCARR3','ATMCARR4','ATMCARR1','BRCA2_INDEX','BRCA2_SISTER','ATMCARR6','BVL_INDEX','BVL_SISTER','CAMYO_INDEX','OVAR12','OVAR14','OVAR16','OVAR3','OVAR5','OVAR7','OVAR9','DENOVO_INDEX','OVAR10'\n" +
            "| calc candidate 0\n" +
            "| calc exlt5 exomesize*lt5\n" +
            "| calc exlt10 exomesize*lt10\n" +
            "| calc exlt15 exomesize*lt15\n" +
            "| calc exlt20 exomesize*lt20\n" +
            "| calc exlt25 exomesize*lt25\n" +
            "| calc exlt30 exomesize*lt30\n" +
            "| calc attribute '95% gene depth>20,90% gene depth>20,85% gene depth>20,95% gene depth>10,90% gene depth>10,85% gene depth>10'+',exlt5,exlt10,exlt15,exlt20,exlt25,exlt30'\n" +
            "| calc values ''+IF(lt20<0.05,'1','0')+','+IF(lt20<0.1,'1','0')+','+IF(lt20<0.15,'1','0')+','+IF(lt10<0.05,'1','0')+','+IF(lt10<0.1,'1','0')+','+IF(lt10<0.15,'1','0') +','+exlt5+','+exlt10+','+exlt15+','+exlt20+','+exlt25+','+exlt30\n" +
            "| calc bases avg_depth*exomesize\n" +
            "| split attribute,values\n" +
            "| group chrom -gc PN,attribute,candidate -fc values,bases,exomesize -sum -count;\n" +
            "\n" +
            "create #allgenecoverage# = gor [#chrgenecoverageindex#] [#chrgenecoveragesubjects#]\n" +
            "| group genome -gc PN,attribute -sum -fc allCount,sum*\n" +
            "| calc avg_depth sum_sum_bases / sum_sum_exomesize\n" +
            "| rename sum_allCount numberOfGenes\n" +
            "| rename sum_sum_exomeSize exomeSize\n" +
            "| calc proportion IF(attribute ~ 'ex*',sum_sum_values,float(sum_sum_values) / numberOfGenes)\n" +
            "| select 1-3,PN,attribute,proportion,numberOfGenes,exomeSize,avg*\n" +
            "| calc analysis IF(attribute ~ 'ex*','exomeCoverage','geneCoverage');\n" +
            "\n" +
            "create #allvarchromstatbaseindex# = pgor #wesVars# -f 'BVL_INDEX'\n" +
            "| calc varType IF(len(reference)=1 and len(call)=1,'SNPs','InDel')\n" +
            "| join -varseg -r -l -e 0 <(gor   ##exons## | select 1-3 | segspan | calc exonic '1' )\n" +
            "| varjoin -r -l -e 'other' <(##VEP## | where isfloat(max_Af) | calc freqRange IF(max_Af<0.001,'veryrare (< 0.1%)',IF(max_Af<=0.01,'rare (0.1% - 1%)',IF(max_Af<=0.05,'medium (1% - 5%)','common (> 5%)')))| select 1-4,Max_Consequence,Max_Impact,freqRange)\n" +
            "| varjoin -r -l -e 'absent' <(##dbsnp## | select 1-4 | calc indbSNP 'present')\n" +
            "| calc transition IF(len(reference)=1 and len(call)=1 and reference+'>'+call in ('A>G','G>A','C>T','T>C'),1,0)\n" +
            "| calc transversion IF(len(reference)=1 and len(call)=1 and transition=0,1,0)\n" +
            "| calc zygosity IF(callCopies=1,'het','hom')\n" +
            "| group chrom -gc PN,CallCopies,Filter,varType,exonic,Max_Impact,freqRange,zygosity,indbSNP -count -ic transition,transversion -sum\n" +
            "| rename sum_(.*) #{1};\n" +
            "\n" +
            "create #allvarchromstatbasesubjects# = pgor #wesVars# -f 'CharcotMT_Father','CharcotMT_Mother','DENOVO_FATHER','DENOVO_MOTHER','OVAR2','OVAR4','OVAR6','OVAR8','OVAR11','OVAR13','AHCFATHER','AHCINDEX','AHCMOTHER','AHCBROTHER','BVL_FATHER','BVL_MOTHER','CAMYO_CASE','OVAR15','ATMCARR2','ATMCARR3','ATMCARR4','ATMCARR1','BRCA2_INDEX','BRCA2_SISTER','ATMCARR6','BVL_INDEX','BVL_SISTER','CAMYO_INDEX','OVAR12','OVAR14','OVAR16','OVAR3','OVAR5','OVAR7','OVAR9','DENOVO_INDEX','OVAR10'\n" +
            "| calc varType IF(len(reference)=1 and len(call)=1,'SNPs','InDel')\n" +
            "| join -varseg -r -l -e 0 <(gor  ##exons## | select 1-3 | segspan | calc exonic '1' )\n" +
            "| varjoin -r -l -e 'other' <(##VEP## | where isfloat(max_Af) | calc freqRange IF(max_Af<0.001,'veryrare (< 0.1%)',IF(max_Af<=0.01,'rare (0.1% - 1%)',IF(max_Af<=0.05,'medium (1% - 5%)','common (> 5%)')))| select 1-4,Max_Consequence,Max_Impact,freqRange)\n" +
            "| varjoin -r -l -e 'absent' <(##dbsnp## | select 1-4 | calc indbSNP 'present')\n" +
            "| calc transition IF(len(reference)=1 and len(call)=1 and reference+'>'+call in ('A>G','G>A','C>T','T>C'),1,0)\n" +
            "| calc transversion IF(len(reference)=1 and len(call)=1 and transition=0,1,0)\n" +
            "| calc zygosity IF(callCopies=1,'het','hom')\n" +
            "| group chrom -gc PN,CallCopies,Filter,varType,exonic,Max_Impact,freqRange,zygosity,indbSNP -count -ic transition,transversion -sum\n" +
            "| rename sum_(.*) #{1};\n" +
            "\n" +
            "create #allvarstatbase# = gor [#allvarchromstatbaseindex#] [#allvarchromstatbasesubjects#]\n" +
            "| where not (chrom IN ('chrX','chrY','chrM','chrXY'))\n" +
            "| where exonic = 1\n" +
            "| group genome -gc PN,CallCopies,Filter,varType,exonic,Max_Impact,freqRange,zygosity,indbSNP -sum -ic allCount-\n" +
            "| rename sum_(.*) #{1};\n" +
            "\n" +
            "def allcalcproportion($1,$2) = [#allvarstatbase#]\n" +
            "| merge <(gor [#allvarstatbase#] | group genome -gc PN | join -segseg -r <([#allvarstatbase#] | group genome -gc $1) | calc allCount '0' )\n" +
            "| group genome -gc PN,$1 -sum -ic allCount\n" +
            "| granno genome -gc PN -sum -ic sum_allCount\n" +
            "| calc attribute $1\n" +
            "| calc proportion 100*float(sum_allCount)/sum_sum_allCount\n" +
            "| rename sum_sum_allcount totalVars\n" +
            "| hide sum*\n" +
            "| calc analysis $2;\n" +
            "\n" +
            "create #allanalysis# = gor [#allgenecoverage#]\n" +
            "| merge <( gor [#allvarstatbase#] | group genome -gc PN -sum -ic transition,transversion | calc proportion float(sum_transition)/sum_transversion | calc totalVars sum_transition+sum_transversion | hide sum* | calc analysis 'transition transversion Analysis' | calc attribute 'tt_ratio' )\n" +
            "| merge <( allcalcproportion(indbsnp,'dbSNP Analysis') )\n" +
            "| merge <( allcalcproportion(zygosity,'zygosity Analysis') )\n" +
            "| merge <( allcalcproportion(freqRange,'frequency Analysis') )\n" +
            "| merge <( allcalcproportion(max_impact,'impact Analysis' ) )\n" +
            "| merge <( allcalcproportion(Filter,'quality Analysis' ) )\n" +
            "| merge <( allcalcproportion(varType,'SNP vs InDel Analysis' ) )\n" +
            "| select 1-3,PN,analysis,attribute,proportion,totalvars,avg_depth,numberOfGenes,exomeSize\n" +
            "| granno genome -gc analysis,attribute -fc proportion,avg_depth -avg -std -count\n" +
            "| rank genome proportion -gc analysis,attribute -o desc -z -q\n" +
            "| rename rank_proportion rank_perc_FromTop\n" +
            "| rename lowOReqRank lowOReqRankFromTop\n" +
            "| hide eqRank\n" +
            "| rank genome proportion -gc analysis,attribute -o asc -q\n" +
            "| rename rank_proportion rank_perc_FromBottom\n" +
            "| rename lowOReqRank lowOReqRankFromBottom\n" +
            "| hide eqRank\n" +
            "| rename allCount PNcount\n" +
            "| calc ratio IF(attribute = 'tt_ratio',proportion,(proportion/100.0)/(1.0-proportion/100.0))\n" +
            "| calc InDistribution IF(z_proportion < -2.0 or lowOReqRankFromBottom <= 0.05,'Low',IF(z_proportion > 2.0 or lowOReqRankFromTop <= 0.05,'High','Norm'))\n" +
            "| calc Color IF(z_proportion < -3.0 or z_proportion > 3.0 ,'Red',IF(z_proportion < -2.0 or z_proportion > 2.0,'Orange','Green'));\n" +
            "\n" +
            "gor [#allanalysis#]\n" +
            "| replace exomeSize IF(exomeSize =\"\",0.0,FLOAT(exomeSize))\n" +
            "| replace proportion IF(attribute ~ 'exlt*',FORM(proportion/exomeSize,1,5),FORM(proportion,4,6))\n" +
            "| replace numberOfgenes IF(numberOfgenes =\"\",0,INT(FLOAT(numberOfgenes)))\n" +
            "| replace avg_proportion IF(avg_proportion=\"\",avg_proportion,form(FLOAT(avg_proportion),4,6))\n" +
            "| replace std_proportion IF(std_proportion=\"\",std_proportion,form(FLOAT(std_proportion),4,6))\n" +
            "| replace ratio form(ratio,4,6)\n" +
            "| replace exomeSize round(exomeSize)\n" +
            "| replace avg_depth IF(avg_depth=\"\",avg_depth,FORM(FLOAT(avg_depth),4,6))\n" +
            "| prefix 3- all\n" +
            "| rename all_PN PN\n" +
            "| rename all_analysis analysis\n" +
            "| rename all_attribute attribute\n" +
            "| distinct\n" +
            "| sort genome";

    static final String Q30 = "def ##dbsnp## = /Users/snorris/gor-services/tests/data/gor/dbsnp_test.gor;\n" +
            "\n" +
            "\n" +
            "gor -p chr1 <(gor ##dbsnp## | varjoin ##dbsnp## -span 500 | sort chrom | group chrom -count\n" +
            "                                | merge -s <(gor ##dbsnp## | varnorm #3 #4 -left  | sort chrom | varjoin ##dbsnp## -span 500 | group chrom -count )\n" +
            "                                | merge -s <(gor ##dbsnp## | varnorm #3 #4 -right | sort chrom | varjoin ##dbsnp## -span 500 | group chrom -count ) )\n" +
            "| group genome -dis -sc allcount\n" +
            "| where dis_allcount != 1 | rownum";

    static final String PIVOTNOR = "create #pns# = norrows 2000\n" +
            "| rename #1 PN\n" +
            "| replace PN 'PN_'+right('00000'+#1,5)\n" +
            "| signature -timeres 1;\n" +
            "\n" +
            "create #buckets# = nor [#pns#]\n" +
            "| rownum\n" +
            "| calc bucket 'b_'+str(div(int(right(pn,5)),73))\n" +
            "| hide rownum\n" +
            "| signature -timeres 1;\n" +
            "\n" +
            "create #vars# = pgor /mnt/csa/env/gortest/projects/sim300/ref/freq_max.gorz\n" +
            "| top 100\n" +
            "| select 1-5\n" +
            "| rownum\n" +
            "| rank  1 rownum -gc ref\n" +
            "| calc p 'f,m'\n" +
            "| split p\n" +
            "| multimap -cartesian -h [#pns#]\n" +
            "| where random() <= max_af\n" +
            "| group 1 -gc ref,pn -lis -sc rank_rownum,allele\n" +
            "| replace lis_allele,lis_rank_rownum listfilter(#rc,'i<=2');\n" +
            "\n" +
            "create #allvars# = gor [#vars#]\n" +
            "| select 1,2,ref,lis_allele,lis_rank_rownum\n" +
            "| split lis_allele,lis_rank_rownum\n" +
            "| distinct\n" +
            "| sort 1 -c ref,lis_rank_rownum:n\n" +
            "| group 1 -gc ref -lis -sc lis_allele\n" +
            "| rename lis_lis_allele alt;\n" +
            "\n" +
            "create #cols# = nor [#pns#]\n" +
            "| top 2000;\n" +
            "\n" +
            "create #pVCF# = nor <(gor [#allvars#] | multimap -cartesian -h [#pns#] | join -snpsnp -l -xl ref,pn -xr ref,pn -r [#vars#] -e '0/0'\n" +
            "| rename lis_allele alt | rename lis_rank_rownum gt\n" +
            "| select 1-alt,pn,gt\n" +
            "| replace gt if(len(gt)=1,'0/'+gt,replace(listsortasc(gt),',','/'))\n" +
            "| replace gt if(right(pn,2)='01','1/1',if(int(right(pn,2))<10,if(random()<0.1*int(right(pn,2)),gt,'./.'),gt))\n" +
            "/*\n" +
            "| map -c pn -h [#buckets#]\n" +
            "| sort 1 -c pn\n" +
            "| group 1 -gc ref,alt,bucket -lis -sc gt -len 1000000\n" +
            "| rename lis_gt values\n" +
            "| csvsel -gc ref,alt -u '3' -vcf [#buckets#] <(nor [#buckets#] | select PN | top 10 )\n" +
            "*/\n" +
            "| pivot pn -gc ref,alt -vf [#cols#]\n" +
            "| rename (.*)_gt #{1} );\n" +
            "\n" +
            "create #emptycov# = gor /mnt/csa/env/gortest/projects/sim300/source/cov/goodcov_10.wes.gord\n" +
            "| top 0;\n" +
            "\n" +
            "create #hvars# = gor [#pVCF#]\n" +
            "| calc format 'GT'\n" +
            "| select 1-alt,format,alt[+1]-format[-1]\n" +
            "| replace alt listmap(alt,'x+\";\"+str(i)')\n" +
            "| replace format[+1]- vcfformattag(format,#rc,'GT')\n" +
            "| hide format\n" +
            "| unpivot alt[+1]-\n" +
            "| rename col_value gt\n" +
            "| rename col_name PN\n" +
            "| split alt\n" +
            "| colsplit alt 2 x -s ';'\n" +
            "| calc y if(left(gt,1)=x_2 and right(gt,1)=x_2,2,if(right(gt,1)=x_2 or left(gt,1)=x_2,1,if(gt='./.',3,0)))\n" +
            "| select 1,2,ref,x_1,y,PN\n" +
            "| rename x_1 Alt\n" +
            "| rename y gt\n" +
            "/*\n" +
            "| gtgen -gc #3,#4 [#buckets#] [#emptycov#]\n" +
            "*/\n" +
            ";\n" +
            "\n" +
            "/*\n" +
            "gor [#hvars#]\n" +
            "| csvsel -gc ref,alt -u '3' -vs 1 [#buckets#] <(nor [#buckets#] | select PN | skip 3 | top 10 )\n" +
            "*/\n" +
            "\n" +
            "create #totalchromvars# = pgor [#hvars#]\n" +
            "| select 1-4\n" +
            "| distinct\n" +
            "| group chrom -count;\n" +
            "\n" +
            "create #totalvars# = nor [#totalchromvars#]\n" +
            "| group -sum -ic allcount\n" +
            "| rename sum_allcount total_vars;\n" +
            "\n" +
            "/*\n" +
            "create #pnchrom# = pgor [#hvars#]\n" +
            "| csvsel -gc ref,alt -vs 1 -u 3 [#buckets#] <(nor [#buckets#] | select PN) -tag PN -hide 0\n" +
            "| group chrom -gc PN,value -count;\n" +
            "*/\n" +
            "\n" +
            "create #pnchrom# = parallel -parts <(nor [#buckets#] | group -gc bucket -lis -sc pn -len 10000 | select lis_pn | replace lis_pn listmap(lis_pn,'\"\\'\"+x+\"\\'\"') )\n" +
            "<(pgor [#hvars#]\n" +
            "| csvsel -gc ref,alt -vs 1 -u 3 [#buckets#] <(nor [#buckets#] | select PN | where pn in ( #{col:lis_pn} )) -tag PN -hide 0 | group chrom -gc PN,value -count);\n" +
            "\n" +
            "create #pn_yield# = nor <(gor [#pnchrom#] | group genome -gc pn,value -sum -ic allcount\n" +
            "| rename sum_allcount varCount)\n" +
            "| select PN,value,varcount\n" +
            "| pivot -gc pn value -v 1,2,3 -e 0\n" +
            "| prefix #2- x\n" +
            "| multimap -cartesian -h [#totalvars#];\n" +
            "\n" +
            "nor [#pn_yield#]\n" +
            "| calc x_0_varcount total_vars - (x_1_varcount + x_2_varcount + x_3_varcount)\n" +
            "| calc yield form(float(int(x_0_varCount) + x_1_varcount + x_2_varCount)/float(int(x_0_varcount) + x_1_varcount + x_2_varcount + x_3_varcount),5,4)\n" +
            "| calc hetFraction form((x_1_varcount)/float(int(x_0_varCount) + x_1_varcount + x_2_varcount),5,4)\n" +
            "| calc homFraction1 form(int(x_2_varcount)/float(int(x_0_varCount) + x_1_varcount + x_2_varcount),5,4)\n" +
            "| calc homFraction2 form((int(x_0_varcount)+x_2_varcount)/float(int(x_0_varCount) + x_1_varcount + x_2_varcount),5,4)\n" +
            "| calc homFraction3 form(int(x_2_varcount)/float(int(x_1_varcount) + x_2_varcount),5,4)\n" +
            "| columnsort PN,yield,het*,hom*;\n";

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            CommandParseUtilities.quoteSafeSplit(MENDEL, '|');
            CommandParseUtilities.quoteSafeSplit(QC, '|');
            CommandParseUtilities.quoteSafeSplit(Q30, '|');
            CommandParseUtilities.quoteSafeSplit(PIVOTNOR, '|');
            CommandParseUtilities.quoteSafeSplit("gor foo bar -h -a <(nor foo1 bar1)", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo bar -h -a <(nor foo1 bar1) gar far", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo \"bar -h -a\" <(nor foo1 bar1) gar far", ' ');
            CommandParseUtilities.quoteSafeSplit("\"gor foo\" \"bar -h -a\" <(nor foo1 bar1) gar far", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo <(nor foo1() bar1)", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo <(nor foo1() bar1 <(git fit))", ' ');
            CommandParseUtilities.quoteSafeSplit("gor \"foo <(nor foo1() bar1 <(git fit))\"", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo <(nor \"foo1() bar1\")", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo <(nor foo1() bar1)friggum", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo <(nor foo1() bar1)", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo <(nor foo1() bar1) <(gor foo1() bar1) <(for foo1() bar1)", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo <(nor foo1() bar1)friggum", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo {nor foo1() bar1}", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo|bar car| goo {gor2 foo2 | gooooossshhh}", ' ');
            CommandParseUtilities.quoteSafeSplit("gor foo", ' ');
            CommandParseUtilities.quoteSafeSplit("gor 'foo'", ' ');
            CommandParseUtilities.quoteSafeSplit("gor (foo)", ' ');
            CommandParseUtilities.quoteSafeSplit("gor <(gor bla)", ' ');
            CommandParseUtilities.quoteSafeSplit("gor '<(gor bla)'", ' ');
            CommandParseUtilities.quoteSafeSplit("gor (if(if(foo)) )", ' ');
            CommandParseUtilities.quoteSafeSplit("gor test2321.gor | calc new substr(replace(old,')',''),0,5)", ' ');
        }
        long end = System.currentTimeMillis();
        long duration = end - start;
        System.out.println(duration);
    }
}
