import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Icon } from './icon';

@Component({
  imports: [Icon],
  template: `<app-icon name="edit" /><app-icon name="trash" />`,
})
class Host {}

describe('Icon', () => {
  it('draws a different picture for each name and hides it from screen readers', async () => {
    await TestBed.configureTestingModule({ imports: [Host] }).compileComponents();
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();

    const svgs = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('svg'));
    expect(svgs).toHaveLength(2);
    expect(svgs.every((svg) => svg.getAttribute('aria-hidden') === 'true')).toBe(true);
    expect(svgs[0].innerHTML).not.toEqual(svgs[1].innerHTML);
  });
});
