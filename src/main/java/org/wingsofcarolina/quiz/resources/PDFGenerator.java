package org.wingsofcarolina.quiz.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import org.wingsofcarolina.quiz.QuizConfiguration;
import org.wingsofcarolina.quiz.common.QuizBuildException;
import org.wingsofcarolina.quiz.domain.Answer;
import org.wingsofcarolina.quiz.domain.Question;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.kernel.pdf.colorspace.PdfColorSpace;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;


public class PDFGenerator {
	
	private Quiz quiz;
	private PdfFont normal;
	private QuizConfiguration config;
	
	public PDFGenerator(QuizConfiguration config, Quiz quiz) throws IOException {
		this.config = config;
		this.quiz = quiz;
		normal = PdfFontFactory.createFont(StandardFonts.HELVETICA, true);
	}
	
	public ByteArrayInputStream generate() throws QuizBuildException, URISyntaxException, MalformedURLException, IOException {		
		// Build the quiz itself
		quiz.build();

		// Create the document
		ByteArrayOutputStream inMemoryStream = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(inMemoryStream);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);
		document.setFont(normal).setFontSize(10);
		PageXofY event = new PageXofY(pdf);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, event);

		// First lets flesh out the document header
		addDocumentHeader(quiz, document);

		// Generate all the questions
		Integer number = 1;
		for (Question question : quiz.getQuestions()) {
			document.add(addQuestion(number, question));
			document.add(new Paragraph("\n"));
			number++;
		}

		// Finalize the document
        event.writeTotal(pdf);
		document.close();

		return new ByteArrayInputStream(inMemoryStream.toByteArray());
	}
	
	private IBlockElement addQuestion(Integer index, Question question) {
		char[] characters = { 'A', 'B', 'C', 'D', 'E' };
		Float CELL_WIDTH = 30.0f;

		Table table = new Table(new float[] {1, 2});
		table.setWidth(UnitValue.createPercentValue(100));
		table.setKeepTogether(true);
		table.setFont(normal);

		Cell cell = new Cell();
		cell.setBorder(Border.NO_BORDER);
		cell.setTextAlignment(TextAlignment.RIGHT);
		cell.add(new Paragraph(index.toString() + " : "));
		cell.setWidth(CELL_WIDTH);
		table.addCell(cell);
		
		cell = new Cell();
		cell.setBorder(Border.NO_BORDER);
		cell.setFontSize(12);
		Paragraph graph = question.getQuestionAsIText();
		String questionId = new Long(question.getQuestionId()).toString();
		graph.add(new Text(" (" + questionId + ")"));
		cell.add(graph);
		table.addCell(cell);
		
		int aIndex = 0;
		for (Answer answer : question.getAnswers()) {
			cell = new Cell();
			cell.setBorder(Border.NO_BORDER);
			cell.setTextAlignment(TextAlignment.RIGHT);
			cell.add(new Paragraph(characters[aIndex] + " : "));
			cell.setWidth(CELL_WIDTH);
			table.addCell(cell);
			
			cell = new Cell();
			cell.setBorder(Border.NO_BORDER);
			cell.add(answer.getAnswerAsIText());
			table.addCell(cell);
			aIndex++;
		}
		return table;
	}
	
	private void addDocumentHeader(Quiz quiz2, Document document) throws URISyntaxException, MalformedURLException, IOException {
		// Generate header
		byte[] bytes = Files.readAllBytes(new File(config.getAssetDirectory() + "/WCFC-logo-transparent.jpg").toPath());
		Image img = new Image(ImageDataFactory.create(bytes));
		img.setWidth(150.0f);
		
		Table table = new Table(new float[]{1, 2});
		table.setWidth(UnitValue.createPercentValue(100));

		// Add WCFC Logo
		Cell cell = new Cell();
		cell.add(img);
		cell.setBorder(Border.NO_BORDER);
		table.addCell(cell);

		// Add quiz details
		Cell cell1 = new Cell();
		cell1.setBorder(Border.NO_BORDER);
		Paragraph p = new Paragraph("WCFC " + quiz.getQuizName() + " Quiz").setFontSize(24.0f);
		p.setTextAlignment(TextAlignment.RIGHT);
		cell1.add(p);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String sunsetDate = sdf.format(quiz.getSunsetDate());
		Paragraph p1 = new Paragraph("Review before : " + sunsetDate);
		p1.setTextAlignment(TextAlignment.RIGHT);
		p1.setFontSize(12);
		cell1.add(p1);
		Paragraph p2 = new Paragraph("Quiz ID : ");
		Text id = new Text(new Long(quiz.getQuizId()).toString()).setBold();
		p2.add(id);
		p2.setTextAlignment(TextAlignment.RIGHT);
		p2.setFontSize(12);
		cell1.add(p2);
		cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
		table.addCell(cell1);
		document.add(table);

		// Add quiz details (i.e. who/what/when/how)
		Table details = new Table(new float[] { 1, 1} );
		details.setWidth(UnitValue.createPercentValue(100));
		details.addCell(new Cell().add(new Paragraph("Instructor : ")));
		details.addCell(new Cell().add(new Paragraph("Date  : ")));
		details.addCell(new Cell().add(new Paragraph("Pilot      : ")));
		details.addCell(new Cell().add(new Paragraph("Score : ")));
		document.add(details);

		// Add instructions
		Paragraph instructor = new Paragraph();
		Text text = new Text("Instructor : ");
		instructor.add(text.setBold());
		instructor.add("Please note the final score (subtract " + quiz.pointsPerQuestion()
				+ " points from 100 for each wrong answer) on the checkout form and file the quiz in "
				+ " the Pilot Records folder.");
		document.add(instructor);
		Paragraph pilot = new Paragraph();
		text = new Text("Pilot : ");
		pilot.add(text.setBold());
		pilot.add("Information required to correctly answer the following questions may be "
				+ " found in FAR Parts 61 and 91, the WCFC SOPs, and club checklists, documents and "
				+ " instructional practices.");
		document.add(pilot);

		SolidLine line = new SolidLine(1f);
		LineSeparator ls = new LineSeparator(line);
		ls.setMargin(5);
		document.add(ls);
	}

	public Cell createTextCell(String text) throws IOException {
	    Cell cell = new Cell();
	    cell.setBorder(Border.NO_BORDER);
	    Paragraph p = new Paragraph(text);
	    p.setTextAlignment(TextAlignment.RIGHT);
	    cell.add(p);
	    cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
	    return cell;
	}
	
    protected class PageXofY implements IEventHandler {
        
        protected PdfFormXObject placeholder;
        protected float side = 20;
        protected float x = 300;
        protected float y = 25;
        protected float space = 4.5f;
        protected float descent = 3;
        
        public PageXofY(PdfDocument pdf) {
            placeholder = new PdfFormXObject(new Rectangle(0, 0, side, side));
        }
        
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdf.getPageNumber(page);
            Rectangle pageSize = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(
                page.getLastContentStream(), page.getResources(), pdf);
            Canvas canvas = new Canvas(pdfCanvas, pdf, pageSize);
            Paragraph p = new Paragraph()
                .add("Page ").add(String.valueOf(pageNumber)).add(" of");
            canvas.showTextAligned(p, x, y, TextAlignment.RIGHT);
            canvas.showTextAligned(new Paragraph("WCFC " + quiz.getQuizName() + " Quiz"), 50, y, TextAlignment.LEFT);
            canvas.showTextAligned(new Paragraph("Quiz ID : " + quiz.getQuizId()), 550, y, TextAlignment.RIGHT);
            pdfCanvas.addXObject(placeholder, x + space, y - descent);
            pdfCanvas.release();
            canvas.close();
        }
        
        public void writeTotal(PdfDocument pdf) {
            Canvas canvas = new Canvas(placeholder, pdf);
            canvas.showTextAligned(String.valueOf(pdf.getNumberOfPages()),
                0, descent, TextAlignment.LEFT);
            canvas.close();
        }
    }
}
