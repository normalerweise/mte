package playground

import edu.stanford.nlp.tagger.maxent.MaxentTagger

/**
 * Created by Norman on 30.06.14.
 */
object PosTest extends App {

  val tagger = new MaxentTagger(
    "resources/stanford_pos_models/english-left3words-distsim.tagger");


  val sample = "Vakıfbank International AGWorld Vakıf UBB Ltd.Kıbrıs Vakıflar Bankası Ltd.Türkiye Sınai Kalkınma Bankası A.Ş.Takasbank İMKB Takas ve Saklama Bankası A.Ş.Güneş Sigorta A.Ş.Vakıf Emeklilik A.Ş.Vakıf Finansal Kiralama A.Ş.Vakıf Menkul Kıymetler Yatırım Ortaklığı A.Ş.Vakıf Gayrimenkul Yatırım Ortaklığı A.Ş.Vakıf Finans Factoring Hizmetleri A.Ş.Vakıf Yatırım Menkul Değerler A.Ş.Vakıf Portföy Yönetimi A.Ş.Kredi Kayıt Bürosu A.Ş. (KKB)Bankalararası Kart Merkezi A.Ş.Kredi Garanti Fonu A.Ş.Taksim Otelcilik A.Ş.Vakıf Gayrimenkul Değerleme A.Ş.Vakıf Enerji ve Madencilik A.Ş.Roketsan Roket Sanayi ve Ticaret A.Ş.Güçbirliği Holding A.Ş.Vakıf Pazarlama Sanayi ve Ticaret A.Ş.İzmir Enternasyonal Otelcilik A.Ş.";

  // The tagged string
  val tagged = tagger.tagString(sample);

  // Output the result
  System.out.println(tagged);

}
