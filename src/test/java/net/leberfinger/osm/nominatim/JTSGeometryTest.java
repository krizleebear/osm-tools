package net.leberfinger.osm.nominatim;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

class JTSGeometryTest {

	@Test
	void readPolygonAsWellKnonwTextFormat() throws ParseException {
		GeometryFactory geoFactory = new GeometryFactory();
		WKTReader wktReader = new WKTReader(geoFactory);
		Geometry gruenwaldGeo = wktReader.read(getGruenwald());
		
		assertEquals(367, gruenwaldGeo.getNumPoints());
		
		double lat = 48.052695626220974;
		double lon = 11.527404785156252;

		// performance: 23s for 1 mio points 
		assertTrue(gruenwaldGeo.contains(geoFactory.createPoint(new Coordinate(lon, lat))));
		assertFalse(gruenwaldGeo.contains(geoFactory.createPoint(new Coordinate(0, 0))));
	}

	
	public static String getGruenwald() {
		return "MULTIPOLYGON(((11.5059379 48.025556,11.5061711 48.025236,11.5069097 48.0257579,11.507518 48.0262713,11.5071872 48.0263725,11.5071807 48.0263642,11.5071262 48.026317,11.5070167 48.0262456,11.506901 48.026181,11.5068841 48.0261735,11.5067927 48.0261334,11.506663 48.0260801,11.5065522 48.0260062,11.5064715 48.0259045,11.5063472 48.0257776,11.5061729 48.0256534,11.5059379 48.025556)),((11.508465 48.028487,11.5086139 48.0285245,11.5091746 48.0286842,11.5095437 48.0287892,11.5097144 48.0288377,11.5099842 48.0295893,11.5100436 48.029755,11.5098403 48.0297669,11.5096497 48.0303623,11.5095967 48.0303762,11.5093686 48.0304352,11.5093348 48.0304158,11.5092886 48.0303736,11.5092528 48.0303205,11.5092281 48.0302719,11.5091399 48.029963,11.5091302 48.0299435,11.5090453 48.0299237,11.5089196 48.0298945,11.5089173 48.0296636,11.508914 48.029341,11.5087717 48.0290036,11.5087632 48.0289787,11.508722 48.0288596,11.5086877 48.0288291,11.5086058 48.0287502,11.508595 48.0287377,11.5084906 48.0286148,11.5084815 48.0285958,11.508465 48.028487)),((11.5092503 48.0359564,11.5094027 48.0357769,11.5095559 48.0355956,11.5097407 48.0353771,11.5098099 48.0352951,11.5098697 48.0352244,11.5099562 48.0351787,11.5100497 48.0351294,11.5104868 48.0348999,11.5107988 48.034736,11.5109208 48.0346197,11.5118053 48.0337773,11.5118543 48.0337305,11.5128927 48.0328211,11.51292 48.0327972,11.5130555 48.0327298,11.5131285 48.0327122,11.5133229 48.032678,11.5135205 48.0326412,11.5137336 48.0326016,11.5141375 48.0325263,11.5163749 48.0322047,11.5181478 48.0319499,11.5181796 48.0319453,11.5183256 48.03191,11.5186835 48.0318235,11.5192847 48.0317561,11.5201355 48.0316606,11.5201162 48.0315667,11.5198958 48.0307611,11.5198874 48.0307298,11.5198635 48.0307072,11.5197978 48.0306447,11.5197652 48.0305216,11.5196096 48.0301949,11.5195328 48.0298403,11.519451 48.0297197,11.5193476 48.0296214,11.519531 48.0295482,11.5197773 48.0294516,11.519899 48.0293974,11.5200266 48.0293601,11.5200986 48.0293422,11.5198463 48.0283369,11.5232909 48.0280964,11.5235424 48.0280788,11.5243026 48.029676,11.5247801 48.0306797,11.5259653 48.0304734,11.530149 48.0297465,11.5301801 48.0297412,11.5303547 48.0298938,11.5311407 48.0305803,11.5312857 48.0307071,11.5313191 48.0307258,11.5314138 48.0308056,11.5324996 48.0319091,11.533108 48.032465,11.5333924 48.0326878,11.5336167 48.0325417,11.5338977 48.0323657,11.5340469 48.0324647,11.5346423 48.0329274,11.5347675 48.0330076,11.5344532 48.0332156,11.5340544 48.0334621,11.5344092 48.0337736,11.5350986 48.0343528,11.5359435 48.0351187,11.5375126 48.0366605,11.5376524 48.0367672,11.5378578 48.0369201,11.5382936 48.0372616,11.5387694 48.0376946,11.5390398 48.0379748,11.5391875 48.0378932,11.539297 48.0380322,11.540344 48.0375363,11.5409756 48.0372171,11.5409805 48.0371892,11.5410415 48.0368442,11.5412406 48.0367173,11.5417325 48.0364455,11.5421898 48.0363445,11.5426104 48.0362445,11.542736 48.0362071,11.5436772 48.035927,11.5442075 48.0357639,11.5457634 48.0352179,11.5461974 48.0350507,11.5471227 48.0347466,11.5479072 48.0345006,11.5482174 48.0344143,11.5495106 48.0341403,11.5507183 48.0339354,11.5510247 48.0338843,11.5517876 48.0337687,11.5518899 48.0337532,11.5521048 48.0337279,11.5525704 48.0336894,11.5531288 48.0337517,11.555461 48.0341567,11.5569028 48.0343242,11.5588681 48.036731,11.559104 48.0369978,11.5592209 48.0371301,11.559699 48.038334,11.5600059 48.0391736,11.5600545 48.0393066,11.5600214 48.0395154,11.5599362 48.0400527,11.5597109 48.0410914,11.5595254 48.0417695,11.5594023 48.0420262,11.5590911 48.0426604,11.558239 48.0436303,11.558013 48.0438125,11.5572752 48.0444632,11.556698 48.0449132,11.5565237 48.0449955,11.55614 48.0451765,11.5556049 48.0452669,11.5549067 48.0452836,11.5544746 48.0451627,11.5538712 48.0449817,11.553243 48.044771,11.5527302 48.0445767,11.552213 48.0443471,11.5518424 48.044049,11.5511329 48.043194,11.5507053 48.0427629,11.550506 48.0425839,11.5502372 48.042343,11.5496312 48.0418124,11.548433 48.0410702,11.548202 48.0409616,11.5479511 48.040839,11.5477372 48.0407356,11.5467208 48.0402848,11.5464429 48.0401387,11.5462108 48.0400169,11.5461037 48.0399573,11.5459735 48.0398853,11.5453437 48.039752,11.54467 48.0396464,11.5445792 48.0396321,11.5436819 48.0395583,11.5430325 48.0395461,11.5425468 48.0395493,11.542104 48.0395762,11.5420871 48.039707,11.5415326 48.0397295,11.5410724 48.0397778,11.5407653 48.03981,11.5415228 48.041233,11.5423571 48.0431228,11.5424082 48.0432539,11.5424131 48.0432663,11.5424968 48.0434021,11.542885 48.044179,11.5429413 48.0443207,11.5429462 48.0443284,11.5429872 48.0443927,11.5433761 48.0453768,11.5435777 48.0458872,11.5439262 48.0467688,11.5439699 48.0468669,11.545852 48.0516902,11.5470152 48.0545972,11.5470408 48.0546807,11.5481541 48.0573258,11.5514534 48.0628065,11.5514647 48.0628252,11.551621 48.0628154,11.5545002 48.0609878,11.554943 48.0612423,11.5561549 48.0619387,11.5584512 48.0634026,11.5580547 48.0636411,11.5575386 48.0640466,11.5561566 48.0648979,11.5545116 48.0659274,11.5543488 48.066011,11.5538446 48.0663827,11.5537476 48.0664827,11.5535655 48.0666917,11.5528575 48.0672524,11.5526205 48.0674573,11.5525085 48.0675289,11.552389 48.0676235,11.5522266 48.0677088,11.5522022 48.0677243,11.5521723 48.0677376,11.5519533 48.0678553,11.5516086 48.0679642,11.5514321 48.0680091,11.5514017 48.0680142,11.5510365 48.0680582,11.5507664 48.0681286,11.5504908 48.0682004,11.5501122 48.0682616,11.5497555 48.0682645,11.5489775 48.0682252,11.5489464 48.0682329,11.5487937 48.0682535,11.5486856 48.0682557,11.5485854 48.06827,11.5485381 48.0682697,11.548492 48.0682621,11.5482975 48.0682708,11.548087 48.0683143,11.5467458 48.0683679,11.5465553 48.0683905,11.5462636 48.0684252,11.5459544 48.068462,11.545907 48.06837,11.5458574 48.0682735,11.5457763 48.0681157,11.5455052 48.0681573,11.5454241 48.0681278,11.5450481 48.0681918,11.5440667 48.0683123,11.5435382 48.0665606,11.5430703 48.0658679,11.5428222 48.0655066,11.542753 48.0653562,11.5426713 48.0652007,11.5425975 48.065083,11.542579 48.0650412,11.5419954 48.0639832,11.5416299 48.0630496,11.5414287 48.0624564,11.5409269 48.0613157,11.5401825 48.0601624,11.5392125 48.0587953,11.5388954 48.0584334,11.538604 48.0581894,11.5382308 48.0579688,11.5381741 48.0579422,11.5380819 48.0580018,11.5379117 48.058112,11.537759 48.0582125,11.5376139 48.0583037,11.5368633 48.0580249,11.5358696 48.0577716,11.5344765 48.0575969,11.5334407 48.0575793,11.5317669 48.0575474,11.5307444 48.0575035,11.5295258 48.0572927,11.5284272 48.0569877,11.5279192 48.0567674,11.5277594 48.0567103,11.5271812 48.0564383,11.5266628 48.0560813,11.5265951 48.0560365,11.5265675 48.0560096,11.5262116 48.0557288,11.5257114 48.055205,11.5253849 48.0548531,11.5249285 48.0543676,11.5247755 48.0541653,11.5245703 48.0538377,11.5245279 48.053775,11.5242256 48.0532772,11.5241696 48.0531718,11.5241298 48.0530971,11.5240223 48.0529169,11.5238713 48.0526637,11.5225488 48.0503067,11.5222078 48.0495547,11.5216903 48.0488288,11.5216691 48.048799,11.5209862 48.0482263,11.5203881 48.0478995,11.5202259 48.047811,11.5193016 48.0474223,11.5183087 48.0469799,11.5173219 48.0465192,11.5167219 48.0460605,11.5162561 48.045425,11.5159804 48.0447904,11.5158522 48.0441811,11.5161301 48.0430432,11.5161302 48.0430203,11.5161081 48.0424404,11.5160119 48.0416637,11.5157559 48.0409479,11.5157323 48.0408822,11.5156065 48.0407091,11.5153067 48.0402973,11.5146985 48.0398081,11.513941 48.0393617,11.5111842 48.0377197,11.5103462 48.0371553,11.5099222 48.0366781,11.5095116 48.0362927,11.5092503 48.0359564)),((11.5147583 48.0130251,11.5150739 48.0130093,11.5153966 48.0129625,11.5154954 48.0134089,11.5149112 48.0134179,11.5147583 48.0130251)),((11.5160421 48.0127824,11.517277 48.0126472,11.5173792 48.012637,11.5176584 48.0126087,11.5176878 48.0127273,11.5180446 48.0141245,11.5163776 48.0142666,11.5161721 48.0133541,11.5160644 48.0129495,11.5160421 48.0127824)),((11.5255327 48.02513,11.5265031 48.0251029,11.5262791 48.0254654,11.5257789 48.025782,11.5257055 48.0255907,11.5255327 48.02513)))";
	}
}