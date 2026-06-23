export default function Button({ name, type="button", onClick, children, disabled = false, title=""}) {
  return (
    <button 
      className={name} 
      type={type}
      onClick={onClick} 
      disabled={disabled}
      title={title}
    >
      {children}
    </button>
  );
}