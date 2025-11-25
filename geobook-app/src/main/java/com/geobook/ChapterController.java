package com.geobook;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/books/{bookId}/chapters")
public class ChapterController {

    private final ChapterRepository chapterRepository;
    private final BookRepository bookRepository;

    public ChapterController(ChapterRepository chapterRepository, BookRepository bookRepository) {
        this.chapterRepository = chapterRepository;
        this.bookRepository = bookRepository;
    }

    @GetMapping
    public String listChapters(@PathVariable Long bookId, Model model) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new IllegalArgumentException("Invalid book Id:" + bookId));
        model.addAttribute("book", book);
        model.addAttribute("chapters", chapterRepository.findByBookBookId(bookId));
        return "chapters";
    }

    @GetMapping("/add")
    public String addChapterForm(@PathVariable Long bookId, Model model) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new IllegalArgumentException("Invalid book Id:" + bookId));
        Chapter chapter = new Chapter();
        chapter.setBook(book);
        model.addAttribute("chapter", chapter);
        return "chapter-form";
    }

    @PostMapping("/add")
    public String addChapter(@PathVariable Long bookId, @ModelAttribute Chapter chapter) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new IllegalArgumentException("Invalid book Id:" + bookId));
        chapter.setBook(book);
        chapterRepository.save(chapter);
        return "redirect:/books/{bookId}/chapters";
    }

    @GetMapping("/{chapterId}/edit")
    public String editChapterForm(@PathVariable Long bookId, @PathVariable Long chapterId, Model model) {
        Chapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new IllegalArgumentException("Invalid chapter Id:" + chapterId));
        model.addAttribute("chapter", chapter);
        return "chapter-form";
    }

    @PostMapping("/{chapterId}/edit")
    public String editChapter(@PathVariable Long bookId, @PathVariable Long chapterId, @ModelAttribute Chapter chapter) {
        chapter.setChapterId(chapterId);
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new IllegalArgumentException("Invalid book Id:" + bookId));
        chapter.setBook(book);
        chapterRepository.save(chapter);
        return "redirect:/books/{bookId}/chapters";
    }

    @PostMapping("/{chapterId}/delete")
    public String deleteChapter(@PathVariable Long bookId, @PathVariable Long chapterId) {
        chapterRepository.deleteById(chapterId);
        return "redirect:/books/{bookId}/chapters";
    }
}
