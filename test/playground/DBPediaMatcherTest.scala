package playground

import scala.util.matching.Regex

/**
 * Created by Norman on 02.07.14.
 */
object DBPediaMatcherTest extends App {

  val valueRegex = """(?iu)[\D]*?(-?\,?[0-9]+(?:[\. ][0-9]{3})*(?:\,[0-9]+)?).*""".r

  val unitRegex = """.*(OMR|Ethiopian birr|BBD|PLN|Jamaican dollar|azerbaijani manat|Riel|Balboa|Nuevo sol|Kip|BMD|South African rand|Peso Uruguayo|TJS|TND|East Caribbean dollar|GNF|SDG|Namibian dollar|PKR|Latvian lats|FKP|MUR|XAF|Russian rouble|SAR|VEF|CAD|Swedish krona|Moldovan leu|HKD|Solomon Islands dollar|PYG|MGA|Barbados dollar|Ouguiya|Samoan tala|AUD|Malaysian ringgit|AMD|Zimbabwe dollar|YER|Haiti gourde|Venezuelan bolívar fuerte|Bermudian dollar|MMK|Kyat|Rial Omani|SEK|TRY|Tunisian dinar|kr|US dollar|CFA Franc BCEAO|Romanian new leu|KES|GEL|GTQ|TZS|Hryvnia|US|Qatari rial|CUP|ALL|ERN|BRL|Djibouti franc|UGX|GIP|MZN|KRW|malawian kwacha|Forint|JOD|Boliviano|IQD|VUV|Lempira|UZS|¥|Convertible marks|UAH|LVL|Yemeni rial|ZWD|PEN|KMF|Mauritius rupee|DOP|BDT|Sudanese pound|LKR|Costa Rican colon|FJD|Lithuanian litas|LSL|United Arab Emirates dirham|Argentine peso|BSD|SRD|azerbaijanian manat|zambian kwacha|Cordoba oro|SHP|LRD|Złoty|Metical|LTL|QAR|Indian rupee|BND|TMT|Dobra|CDF|Danish krone|STD|Egyptian pound|SZL|CZK|Croatian kuna|δολάρια|Vatu|Pataca|BGN|Bahamian dollar|Hong Kong dollar|Iranian rial|JMD|Jordanian dinar|UYU|NPR|Somoni|Cape Verde escudo|Paanga|EGP|Lari|AZN|Swiss franc|CLP|Guinea franc|Somali shilling|Rupiah|MOP|δολάριο|Fiji dollar|SCR|HTG|CFA franc BEAC|LAK|Guyana dollar|BTN|GBP|Brunei dollar|TWD|DZD|CFP franc|Israeli new sheqel|Renminbi|RUR|New Taiwan dollar|MXN|Tugrik|Surinam dollar|AWG|THB|ISK|LBP|SGD|Lilangeni|MWK|Bulgarian lev|Ngultrum|Pound sterling|WST|DJF|Naira|KZT|CRC|LYD|NGN|naira|BIF|CHF|RWF|AED|turkmenistani manat|Falkland Islands pound|Pula|Comoro franc|INR|Netherlands Antillean guilder|XOF|Colombian peso|Chilean peso|MRO|Seychelles rupee|Zloty|EEK|Belarussian ruble|Serbian dinar|Dominican peso|Afghani|Ευρώ|PGK|CNY|Sri Lanka rupee|PHP|MDL|Kuwaiti dinar|SYP|RON|Quetzal|AFN|Iceland krona|KHR|Slovak koruna|Costa Rican colón|Australian dollar|COP|DKK|€|Pakistan rupee|Guarani|KYD|XPF|C|Cuban peso|GMD|Kina|turkish lira|MVR|Aruban guilder|Rwanda franc|TTD|Algerian dinar|PAB|Saudi riyal|South Korean won|Singapore dollar|SKK|British Pound|Leone|JPY|Denar|TOP|BWP|Cayman Islands dollar|MKD|Dalasi|ARS|Brazilian real|HUF|Bahraini dinar|MYR|Euro|Som|USD|SLL|Japanese yen|MAD|RUB|Libyan dinar|MNT|BOB|GYD|Dollar|SBD|Syrian pound|Tenge|BHD|Tanzanian shilling|HNL|Mexican peso|Moroccan dirham|Lebanese pound|XCD|New Zealand dollar|Kroon|Malagasy ariary|Rufiyaa|NZD|Saint Helena pound|KGS|AOA|Baht|BZD|Bangladeshi taka|North Korean won|IDR|SOS|\\$|NIO|Philippine peso|GHS|BYR|ANG|RSD|ILS|Lek|NOK|Franc Congolais|Loti|KWD|NAD|ETB|Uganda shilling|Nakfa|Armenian dram|Czech koruna|Burundian franc|Belize dollar|KPW|£|Norwegian krone|Cedi|EUR|CVE|ZAR|Gibraltar pound|Uzbekistan som|IRR|ZMK|Kwanza|HRK|Nepalese rupee|BAM|Liberian dollar|Iraqi dinar|Omani rial|Trinidad and Tobago dollar|yen|Kenyan shilling|Canadian dollar).*""".r


  val str = "126400000 GBP (2008)"

  str match {
    case unitRegex(unit) => println(unit)
    case _ => println("no match")
  }

  val m = unitRegex.pattern.matcher(str)
  println(m.matches())
  println(m.groupCount())
  println(m.group(0))
  println(m.group(1))

  str match {
    case valueRegex(value) => println(value)
    case _ => println("no match")
  }

  println("stop")

}
