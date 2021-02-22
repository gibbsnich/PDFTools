# PDFTools

A simple Android app that extracts pages and/or merges complete PDF documents.  

To use it:  
- Select pdf documents to extract pages from or to merge  
- Enter a pdftk like statement to select what to include in the final PDF document  
- Select a target folder where the final PDF document will be stored.  

### pdftk Syntax
Because I could not think of a better way I used [pdftk's syntax](https://www.pdflabs.com/docs/pdftk-cli-examples/) for selecting documents or parts of it. Selected documents correspond to a letter in alphabetical order (first document is A, second is B, etc.), its pages are numbers (first page is 1, second page is 2, etc.) so to select pages that will be contained in the final PDF document you use three types of expressions:  
- to select a complete document, use its letter  
- to selected a single page from a document, use the document's letter and the page's page number (e.g. A2: second page of first document),  
- to select a range of pages from a document, use the document's letter and the first page's page number followed by a `-` character followed by the last page's page number (e.g. A2-5: page 2, 3, 4 of first document will be included in final document).  
  
So, e.g., if you have three documents and want to have a final document containing:
- the first page of the first document (`A1`),  
- page 5, 6 and 7 and page 10-20 from the second document (`B5-8 B10-21`),  
- the complete third document (`C`).  

Then you just enter:  
`A1 B5-8 B10-21 C`  


