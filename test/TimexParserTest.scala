import extractors.{TemporalDBPediaMappingExtractorWrapper, ThreadUnsafeDependencies}
import org.joda.time.DateTime
import models.{Page, Revision}
import play.api.test.FakeApplication
import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable._

class TimexParserTest extends Specification {

  "Extract" should {
    "fin history" in new WithApplication {


      val content =
        """
         {{Infobox NFL player
          ||name=Anquan Boldin
          ||image=Anquan Boldin at McDaniel College in 2010.JPG
          ||caption=Boldin at [[McDaniel College]] in 2010.
          ||currentteam=San Francisco 49ers
          ||currentnumber=81
          ||position=[[Wide receiver]]
          ||birth_date={{Birth date and age|1980|10|3|mf=y}}
          ||birth_place=[[Pahokee, Florida]]
          ||heightft=6
          ||heightin=1
          ||weight=222
          ||debutyear=2003
          ||debutteam=Arizona Cardinals
          ||highlights=
          |* [[Super Bowl|Super Bowl Champion]] ([[Super Bowl XLVII|XLVII]])
          |* [[AFC Championship Game|AFC Champion]] ([[2012–13 NFL playoffs|2012]])
          |* [[NFC Championship Game|NFC Champion]] ([[2008–09 NFL playoffs|2008]])
          |* 3× [[Pro Bowl]] ([[2004 Pro Bowl|2003]], [[2007 Pro Bowl|2006]], [[2009 Pro Bowl|2008]])
          |* [[NFL Offensive Rookie of the Year Award|''AP'' NFL Offensive Rookie of the Year]] (2003)
          |* [[Pro Football Writers Association|PFWA Offensive Rookie of the Year]] (2003)
          |* ''[[USA Today]]'' High School All-American ([[1998 USA Today All-USA high school football team|1998]])
          ||highschool=[[Pahokee High School|Pahokee (FL)]]
          ||college=[[Florida State Seminoles football|Florida State]]
          ||draftyear=2003
          ||draftround=2
          ||draftpick=54
          ||pastteams=
          |* [[Arizona Cardinals]] ({{NFL Year|2003}}–{{NFL Year|2009}})
          |* [[Baltimore Ravens]] ({{NFL Year|2010}}–{{NFL Year|2012}})
          |* [[San Francisco 49ers]] ({{NFL Year|2013}}–present)
          ||statweek=17
          ||statseason=2013
          ||statlabel1=Receptions
          ||statvalue1=857
          ||statlabel2=Receiving yards
          ||statvalue2=11,344
          ||statlabel3=[[Touchdowns|Receiving TD]]s
          ||statvalue3=65
          ||nfl=BOL283010
          |}}
        """.stripMargin


      val page = Page(0, "test", "test")
      val rev = Revision(0, new DateTime, page, content)


      val dependencies =  ThreadUnsafeDependencies.create("tester")
      val extractor = new TemporalDBPediaMappingExtractorWrapper(dependencies)

      val result = extractor.extract(rev)
      println(result.mkString("\n"))
    }

  }
}


object TestTimexParser extends App {


//  val parser = new AdvancedTimexParser()
//
//  val node  = TextNode("blah 1940",1)
//  val result = parser.parse(node, "blah")
//
//  println(result)

  //val dt = new DateTime();
  //val fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
  //val  dt = fmt.parseDateTime("2010");
  //val str ="2010-10"
  //println(str.take(4))





}