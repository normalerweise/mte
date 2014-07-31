
import org.dbpedia.extraction.dataparser.ParserUtils
import org.dbpedia.extraction.util.Language
import org.specs2.mutable._

class NumberStringNormalizerSpec extends Specification {


  "A number string with scale info" should {
    "be normalized" in {
      implicit val text = "5 billion"
      processAndCheck("5000000000", "5 billion")
    }

    "be normalized if it has surrounding strings" in {
      implicit val text = "haha 5 billion end"
      processAndCheck("haha 5000000000 end", "5 billion")
    }
  }

  "A number string with thousands separators" should {
    "be normalized if separator is ," in {
      implicit val text = "5,000"
      processAndCheck("5000", "5,000")
    }

    "be normalized if separator is ," in {
      implicit val text = "5,000,000"
      processAndCheck("5000000", "5,000,000")
    }

    "be normalized if separator is ." in {
      implicit val text = "5.000"
      processAndCheck("5000", "5.000")
    }

    "be normalized if separator is ." in {
      implicit val text = "5.000.000"
      processAndCheck("5000000", "5.000.000")
    }

    "be normalized if separator is . with scale value" in {
      implicit val text = "5.000 billion"
      processAndCheck("5000000000", "5.000 billion")
    }

    "be normalized if separator is . with scale value" in {
      implicit val text = "5.00 billion"
      processAndCheck("5000000000", "5.00 billion")
    }

    "be normalized if separator is . with scale value" in {
      implicit val text = "5.0 billion"
      processAndCheck("5000000000", "5.0 billion")
    }

    "be normalized if all aspects occur at once" in {
      implicit val text = "5.000.000.100 million"
      processAndCheck("5000000100000000", "5.000.000.100 million")
    }
  }

  "A number string " should {
    "be normalized if the fraction is grater than the scale" in {
      implicit val text = "5.54321 thousand"
      processAndCheck("5543.21", "5.54321 thousand")
    }

    "be normalized if all aspects occur at once" in {
      implicit val text = "haha €5.000,000,1 million irgendwas"
      processAndCheck("haha €5000000100000 irgendwas", "5.000,000,1 million")
    }

    "be ..." in {
      implicit val text = "haha 5,0 irgendwas"
      processAndCheck("haha 5.0 irgendwas", "5,0")
    }

    "be ..." in {
      implicit val text = "haha 5,00 irgendwas"
      processAndCheck("haha 5.00 irgendwas", "5,00")
    }

    "be ..." in {
      implicit val text = "haha 5,000 irgendwas"
      processAndCheck("haha 5000 irgendwas", "5,000")
    }

    "be ..." in {
      implicit val text = "haha 5,0000 irgendwas"
      processAndCheck("haha 5.0000 irgendwas", "5,0000")
    }
  }



    private def processAndCheck(expValue: String, expSurfaceValue: String)(implicit text: String) = {
    val (number, surfaceString) = p(text)
    number must beEqualTo(expValue)
    surfaceString must beEqualTo(expSurfaceValue)
  }

  // process
  val parserUtils = new ParserUtils(new {def language = Language.English})
  private def p(text: String) = parserUtils.convertLargeNumbersWithSurfaceStringInfo(text)


}
