package org.wingsofcarolina.quiz.domain.presentation;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.common.QuizBuildException;
import org.wingsofcarolina.quiz.domain.Answer;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.resources.Quiz;
import org.wingsofcarolina.quiz.resources.QuizContext;

public class PDFGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(PDFGenerator.class);

  private Font normal;
  private QuizContext context;

  public PDFGenerator(QuizContext context) throws IOException {
    this.context = context;
    normal = FontFactory.getFont("Helvetica", 10f, Font.NORMAL);
  }

  public ByteArrayInputStream generate(Quiz quiz)
    throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {
    if (quiz != null) {
      return generateQuiz(quiz);
    } else {
      throw new QuizBuildException("Null pointer for quiz entity");
    }
  }

  public ByteArrayInputStream generate(Question question)
    throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {
    if (question != null) {
      return generateQuestion(question);
    } else {
      throw new QuizBuildException("Null pointer for question entity");
    }
  }

  public ByteArrayInputStream generate(List<Question> questions)
    throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {
    if (questions != null) {
      return generateQuestion(questions);
    } else {
      throw new QuizBuildException("Null pointer for question entity");
    }
  }

  public ByteArrayInputStream generateQuestion(Question question)
    throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {
    // Create the document
    ByteArrayOutputStream inMemoryStream = new ByteArrayOutputStream();
    Document document = new Document();
    try {
      PdfWriter writer = PdfWriter.getInstance(document, inMemoryStream);
      document.setMargins(36f, 36f, 36f, 50f);
      document.open();
      LineSeparator ls = new LineSeparator();
      ls.setLineWidth(0.5f);
      Paragraph sepTop = new Paragraph();
      sepTop.add(ls);
      sepTop.setSpacingBefore(5f);
      sepTop.setSpacingAfter(5f);
      document.add(sepTop);

      document.add(addQuestion(0, question));

      Paragraph sepBottom = new Paragraph();
      sepBottom.add(ls);
      sepBottom.setSpacingBefore(5f);
      sepBottom.setSpacingAfter(5f);
      document.add(sepBottom);
    } catch (DocumentException e) {
      throw new QuizBuildException("Error generating question PDF", e);
    } finally {
      document.close();
    }

    return new ByteArrayInputStream(inMemoryStream.toByteArray());
  }

  public ByteArrayInputStream generateQuestion(List<Question> questions)
    throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {
    // Create the document
    ByteArrayOutputStream inMemoryStream = new ByteArrayOutputStream();
    Document document = new Document();
    try {
      PdfWriter writer = PdfWriter.getInstance(document, inMemoryStream);
      document.setMargins(36f, 36f, 36f, 50f);
      document.open();
      LineSeparator ls = new LineSeparator();
      ls.setLineWidth(0.5f);
      Paragraph sepTop = new Paragraph();
      sepTop.add(ls);
      sepTop.setSpacingBefore(5f);
      sepTop.setSpacingAfter(5f);
      document.add(sepTop);

      int i = 1;
      for (Question question : questions) {
        document.add(addQuestion(i++, question));
        Paragraph sep = new Paragraph();
        sep.add(ls);
        sep.setSpacingBefore(5f);
        sep.setSpacingAfter(5f);
        document.add(sep);
      }
    } catch (DocumentException e) {
      throw new QuizBuildException("Error generating questions PDF", e);
    } finally {
      document.close();
    }

    return new ByteArrayInputStream(inMemoryStream.toByteArray());
  }

  public ByteArrayInputStream generateQuiz(Quiz quiz)
    throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {
    // Create the document
    ByteArrayOutputStream inMemoryStream = new ByteArrayOutputStream();
    Document document = new Document();
    try {
      PdfWriter writer = PdfWriter.getInstance(document, inMemoryStream);
      document.setMargins(36f, 36f, 36f, 50f);
      PageXofY event = new PageXofY(quiz);
      writer.setPageEvent(event);
      document.open();
      // First lets flesh out the document header
      addDocumentHeader(quiz, document);

      // Generate all the questions
      Integer number = 1;
      for (Question question : quiz.getQuestions()) {
        document.add(addQuestion(number, question));
        Paragraph spacer = new Paragraph();
        spacer.add(Chunk.NEWLINE);
        document.add(spacer);
        number++;
      }
    } catch (DocumentException e) {
      throw new QuizBuildException("Error generating quiz PDF", e);
    } finally {
      document.close();
    }

    return new ByteArrayInputStream(inMemoryStream.toByteArray());
  }

  private PdfPTable addQuestion(Integer index, Question question) {
    char[] characters = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L' };
    Float CELL_WIDTH = 30.0f;

    PdfPTable table = new PdfPTable(2);
    try {
      table.setWidths(new float[] { 1, 15 });
    } catch (DocumentException e) {
      // ignore, use default widths
    }
    table.setWidthPercentage(100);
    table.setKeepTogether(true);

    PdfPCell cell = new PdfPCell();
    cell.setBorder(PdfPCell.NO_BORDER);
    Paragraph idx = new Paragraph(
      index.toString() + " : ",
      FontFactory.getFont("Helvetica", 12f, Font.NORMAL)
    );
    idx.setAlignment(Element.ALIGN_RIGHT);
    cell.addElement(idx);
    cell.setFixedHeight(CELL_WIDTH);
    table.addCell(cell);

    cell = new PdfPCell();
    cell.setBorder(PdfPCell.NO_BORDER);
    // Render question content with 12pt font
    List<Element> qElems = question.getQuestionElements();
    for (Element e : qElems) {
      cell.addElement(e);
    }
    //Remove the question number from the PDF once we go live
    //String questionId = new Long(question.getQuestionId()).toString();
    //graph.add(new Text(" (" + questionId + ")"));
    table.addCell(cell);

    // Add an image attachment, if one is requested
    if (question.getAttachment() != null && !question.getAttachment().equals("NONE")) {
      String imageDir = context.getConfiguration().getImageDirectory() + "/";
      try {
        File iFile = new File(imageDir + question.getAttachment());
        if (iFile.exists()) {
          Image image = Image.getInstance(imageDir + question.getAttachment());
          float imageWidth = image.getWidth();
          if (imageWidth > 400f) {
            float scale = imageWidth / 450.0f;
            float imageHeight = image.getHeight() / scale;
            image.scaleAbsolute(450f, imageHeight);
          }
          cell = new PdfPCell();
          cell.setBorder(PdfPCell.NO_BORDER);
          Paragraph nl = new Paragraph();
          nl.add(Chunk.NEWLINE);
          cell.addElement(nl);
          table.addCell(cell);
          cell = new PdfPCell();
          cell.setBorder(PdfPCell.NO_BORDER);
          image.setAlignment(Element.ALIGN_CENTER);
          cell.addElement(image);
          table.addCell(cell);
        } else {
          LOG.error(
            "Could not find requested image attachment : {}",
            question.getAttachment()
          );
        }
      } catch (IOException e) {
        LOG.error(
          "Exception occured trying to access image file : {}",
          question.getAttachment()
        );
      }
    }

    if (question.isMultipleChoice()) {
      int aIndex = 0;
      if (question.getAnswers() == null || question.getAnswers().size() == 0) {
        cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        Paragraph nl = new Paragraph();
        nl.add(Chunk.NEWLINE);
        nl.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(nl);
        table.addCell(cell);

        cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        Paragraph none = new Paragraph(
          "This question has no answers at this time.",
          FontFactory.getFont("Helvetica", 12f, Font.NORMAL)
        );
        cell.addElement(none);
        table.addCell(cell);
      } else {
        for (Answer answer : question.getAnswers()) {
          cell = new PdfPCell();
          cell.setBorder(PdfPCell.NO_BORDER);
          Paragraph lab = new Paragraph(
            characters[aIndex] + " : ",
            FontFactory.getFont("Helvetica", 10f, Font.NORMAL)
          );
          lab.setAlignment(Element.ALIGN_RIGHT);
          cell.addElement(lab);
          table.addCell(cell);

          cell = new PdfPCell();
          cell.setBorder(PdfPCell.NO_BORDER);
          List<Element> elems = answer.getAnswerElements();
          for (Element e : elems) {
            cell.addElement(e);
          }
          table.addCell(cell);
          aIndex++;
        }
      }
    }
    return table;
  }

  private void addDocumentHeader(Quiz quiz, Document document)
    throws URISyntaxException, MalformedURLException, IOException, DocumentException {
    // Generate header
    byte[] bytes = Files.readAllBytes(
      new File(
        context.getConfiguration().getAssetDirectory() + "/WCFC-logo-transparent.jpg"
      )
        .toPath()
    );
    Image img = Image.getInstance(bytes);
    img.scaleToFit(150.0f, 150.0f);

    PdfPTable table = new PdfPTable(2);
    try {
      table.setWidths(new float[] { 1, 2 });
    } catch (DocumentException e) {
      // ignore, use default widths
    }
    table.setWidthPercentage(100);

    // Add WCFC Logo
    PdfPCell cell = new PdfPCell();
    cell.addElement(img);
    cell.setBorder(PdfPCell.NO_BORDER);
    table.addCell(cell);

    // Add quiz details
    PdfPCell cell1 = new PdfPCell();
    cell1.setBorder(PdfPCell.NO_BORDER);
    Paragraph p = new Paragraph(
      quiz.getQuizName(),
      FontFactory.getFont("Helvetica", 24.0f, Font.NORMAL)
    );
    p.setAlignment(Element.ALIGN_RIGHT);
    cell1.addElement(p);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String sunsetDate = sdf.format(quiz.getSunsetDate());
    Paragraph p1 = new Paragraph(
      "Review before : " + sunsetDate,
      FontFactory.getFont("Helvetica", 12f, Font.NORMAL)
    );
    p1.setAlignment(Element.ALIGN_RIGHT);
    cell1.addElement(p1);
    Paragraph p2 = new Paragraph();
    p2.add(new Chunk("Quiz ID : ", FontFactory.getFont("Helvetica", 12f, Font.NORMAL)));
    p2.add(
      new Chunk(
        Long.valueOf(quiz.getQuizId()).toString(),
        FontFactory.getFont("Helvetica", 12f, Font.BOLD)
      )
    );
    p2.setAlignment(Element.ALIGN_RIGHT);
    cell1.addElement(p2);
    cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
    table.addCell(cell1);
    table.setSpacingAfter(6f);
    document.add(table);

    // Add quiz details (i.e. who/what/when/how) - 2x3 table matching original layout
    PdfPTable details = new PdfPTable(3);
    details.setWidthPercentage(100);

    details.addCell(new PdfPCell(new Phrase("Instructor :")));
    details.addCell(new PdfPCell(new Phrase("")));
    details.addCell(new PdfPCell(new Phrase("Date  :")));

    details.addCell(new PdfPCell(new Phrase("Pilot    :")));
    details.addCell(new PdfPCell(new Phrase("Member # :")));
    details.addCell(new PdfPCell(new Phrase("Score :")));

    details.setSpacingAfter(6f);
    document.add(details);

    // Add instructions
    Paragraph instructor = new Paragraph();
    instructor.add(
      new Chunk("Instructor : ", FontFactory.getFont("Helvetica", 10f, Font.BOLD))
    );
    instructor.add(
      new Chunk(
        "Please note the final score (subtract " +
        quiz.pointsPerQuestion() +
        " points from 100 for each wrong answer) on the checkout form and file the quiz in the Pilot Records folder.",
        FontFactory.getFont("Helvetica", 10f, Font.NORMAL)
      )
    );
    document.add(instructor);
    //		Paragraph pilot = new Paragraph();
    //		text = new Text("Pilot : ");
    //		pilot.add(text.setBold());
    //		pilot.add("Information required to correctly answer the following questions may be "
    //				+ " found in FAR Parts 61 and 91, the WCFC SOPs, and club checklists, documents and "
    //				+ " instructional practices.");
    //		document.add(pilot);

    // Add any Front Matter that might have been configured
    if (context.getVariable("instructions") != null) {
      Paragraph instructions = new Paragraph();
      instructions.add(
        new Chunk("Pilot : ", FontFactory.getFont("Helvetica", 10f, Font.BOLD))
      );
      instructions.add(
        new Chunk(
          String.valueOf(context.getVariable("instructions")),
          FontFactory.getFont("Helvetica", 10f, Font.NORMAL)
        )
      );
      document.add(instructions);
    }

    LineSeparator ls = new LineSeparator();
    ls.setLineWidth(0.5f);
    Paragraph sep = new Paragraph();
    sep.add(ls);
    sep.setSpacingBefore(10f);
    sep.setSpacingAfter(15f);
    document.add(sep);
  }

  public PdfPCell createTextCell(String text) throws IOException {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(PdfPCell.NO_BORDER);
    Paragraph p = new Paragraph(text, FontFactory.getFont("Helvetica", 10f, Font.NORMAL));
    p.setAlignment(Element.ALIGN_RIGHT);
    cell.addElement(p);
    return cell;
  }

  protected class PageXofY extends PdfPageEventHelper {

    protected PdfTemplate total;
    protected float side = 20;
    protected float x = 300;
    protected float y = 30;
    protected float space = 4.5f;
    protected float descent = 3;
    protected Quiz quiz;
    protected Font footerFont = FontFactory.getFont("Helvetica", 10f, Font.NORMAL);
    protected Font smallFont = FontFactory.getFont("Helvetica", 8f, Font.NORMAL);

    public PageXofY(Quiz quiz) {
      this.quiz = quiz;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
      total = writer.getDirectContent().createTemplate(side, side);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      int pageNumber = writer.getPageNumber();
      com.lowagie.text.Rectangle pageSize = document.getPageSize();
      ColumnText.showTextAligned(
        writer.getDirectContent(),
        Element.ALIGN_RIGHT,
        new Phrase("Page " + pageNumber + " of ", footerFont),
        x,
        y,
        0
      );
      ColumnText.showTextAligned(
        writer.getDirectContent(),
        Element.ALIGN_LEFT,
        new Phrase("WCFC " + quiz.getQuizName() + " Quiz", smallFont),
        50,
        y,
        0
      );
      ColumnText.showTextAligned(
        writer.getDirectContent(),
        Element.ALIGN_RIGHT,
        new Phrase("Quiz ID : " + quiz.getQuizId(), smallFont),
        550,
        y,
        0
      );
      writer.getDirectContent().addTemplate(total, x + space, y - descent);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
      ColumnText.showTextAligned(
        total,
        Element.ALIGN_LEFT,
        new Phrase(String.valueOf(writer.getPageNumber() - 1), footerFont),
        0,
        descent,
        0
      );
    }
  }
}
