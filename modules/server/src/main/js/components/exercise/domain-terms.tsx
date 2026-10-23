import {Popover, PopoverContent, PopoverTrigger,} from '@radix-ui/react-popover';
import parse, {DOMNode, Element} from 'html-react-parser';
import {X} from 'lucide-react';
import React, {useState} from 'react';


export function ParsedMessage({ html }: { html: string }) {
  const transform = (node: DOMNode) => {
    if (
      node.type === 'tag' &&
      (node as Element).name === 'span' &&
      (node as Element).attribs?.class?.includes('domain-term')
    ) {
      const el = node as Element;
      const explanation = el.attribs['data-explanation'];

      const child = el.children?.[0];
      let text = '';

      if (child?.type === 'text') {
        text = (child as unknown as Text).data;
      }

      return <DomainTerm term={text} explanation={explanation}></DomainTerm>;
    }
  };

  return <>{parse(html, { replace: transform })}</>;
}

export function DomainTerm({
  term,
  explanation,
}: {
  term: string;
  explanation: string;
}) {
  const [open, setOpen] = useState(false);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <span
          className='compph-domain-term text-decoration-underline'
          style={{ cursor: 'pointer' }}
        >
          {term}
        </span>
      </PopoverTrigger>

      <PopoverContent
        className='popover bs-popover-auto border rounded shadow-sm bg-white p-3'
        style={{ maxWidth: '320px', position: 'relative' }}
      >
        {/* Кнопка закрытия в правом верхнем углу */}
        <button
          type='button'
          className='btn btn-link p-0 border-0 text-muted position-absolute'
          style={{ top: '0.5rem', 
            right: '0.5rem',
            outline: "none",
            boxShadow: "none",
            userSelect: "none",}}
          onClick={() => setOpen(false)}
          aria-label='Close'
        >
          <X size={16} />
        </button>

        {/* Текст объяснения */}
        <p className='mb-0 text-muted' style={{ paddingRight: '1.5rem' }}>
          {explanation}
        </p>
      </PopoverContent>
    </Popover>
  );
}
